package uk.ac.diamond.daq.persistence.json.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.diamond.daq.persistence.data.ItemContainer;
import uk.ac.diamond.daq.persistence.data.ItemReference;
import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.json.JsonDeserialiser;
import uk.ac.diamond.daq.persistence.service.PersistenceException;
import uk.ac.diamond.daq.persistence.service.impl.AbstractPersistenceService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultJsonDeserialiser implements JsonDeserialiser {
    private ObjectMapper objectMapper;
    private AbstractPersistenceService persistenceService;
    private List<PersistableItem> cache;
    private String visitId;

    DefaultJsonDeserialiser(AbstractPersistenceService persistenceService, String visitId) {
        this.objectMapper = new ObjectMapper();
        this.persistenceService = persistenceService;
        this.cache = new ArrayList<>();
        this.visitId = visitId;
    }

    private PersistableItem getPersistableItemFromJsonNode(JsonNode node) throws PersistenceException {
        if (node instanceof ObjectNode) {
            try {
                ItemReference itemReference = objectMapper.treeToValue(node, ItemReference.class);
                for (PersistableItem item : cache) {
                    if (item.getId() == itemReference.getId()) {
                        return item;
                    }
                }
                return persistenceService.get(itemReference, this, visitId);
            } catch (IOException e) {
                //Do nothing as we will return null if not found
            }
        }
        return null;
    }

    private void deserializeArray(List<ObjectPath> objectPaths, ObjectPath objectPath, ArrayNode arrayNode)
            throws PersistenceException {
        Iterator<JsonNode> iterator = arrayNode.iterator();

        List<Integer> toRemove = new ArrayList<>();

        for (int i = 0; iterator.hasNext(); i++) {
            JsonNode arrayItemNode = iterator.next();
            if (arrayItemNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                PersistableItem fieldItem = getPersistableItemFromJsonNode(arrayItemNode);
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    toRemove.add(i);
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) arrayItemNode);
                }
            } else if (arrayItemNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, i);
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) arrayItemNode);
            }
        }

        toRemove.forEach((i) -> {
            arrayNode.remove(i);
            arrayNode.insertNull(i);
        });
    }

    private void deserializeObject(List<ObjectPath> objectPaths, ObjectPath objectPath, ObjectNode objectNode)
            throws PersistenceException {
        Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            JsonNode childNode = entry.getValue();

            if (childNode instanceof ObjectNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                PersistableItem fieldItem = getPersistableItemFromJsonNode(entry.getValue());
                if (fieldItem != null) {
                    childObjectPath.setItem(fieldItem);
                    objectPaths.add(childObjectPath);
                    objectNode.put(entry.getKey(), JsonNodeFactory.instance.nullNode());
                } else {
                    deserializeObject(objectPaths, childObjectPath, (ObjectNode) entry.getValue());
                }
            } else if (childNode instanceof ArrayNode) {
                ObjectPath childObjectPath = new ObjectPath(objectPath, entry.getKey());
                deserializeArray(objectPaths, childObjectPath, (ArrayNode) childNode);
            }
        }
    }

    @Override
    public <T extends PersistableItem> T deserialise(ItemContainer itemContainer) throws PersistenceException {
        for (PersistableItem item : cache) {
            if (item.getId() == itemContainer.getId()) {
                return (T) item;
            }
        }
        try {
            List<ObjectPath> objectPaths = new ArrayList<>();
            ObjectPath objectPath = new ObjectPath();
            ObjectNode baseNode = (ObjectNode) objectMapper.readTree(itemContainer.getJson());
            deserializeObject(objectPaths, objectPath, baseNode);

            T item = (T) objectMapper.treeToValue(baseNode, itemContainer.getItemClass());

            for (ObjectPath foundObjectPath : objectPaths) {
                foundObjectPath.applyTo(item);
            }
            cache.add(item);
            return item;
        } catch (IOException | IllegalAccessException e) {
            throw new PersistenceException("Unable to deserialize item " + itemContainer.getJson()
                    + " class " + itemContainer.getItemClass().getCanonicalName(), e);
        }
    }

    @Override
    public List<PersistableItem> getCache() {
        return cache;
    }
}
