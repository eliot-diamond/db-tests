package uk.ac.diamond.daq.persistence.json.impl;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ObjectPath {
    private List<PathElement> path;
    private PersistableItem fieldItem;

    ObjectPath() {
        path = new ArrayList<>();
        fieldItem = null;
    }

    private ObjectPath(ObjectPath source) {
        this();
        for (PathElement sourcePathElement : source.path) {
            PathElement pathElement = new PathElement();
            pathElement.type = sourcePathElement.type;
            pathElement.arrayIndex = sourcePathElement.arrayIndex;
            pathElement.fieldName = sourcePathElement.fieldName;
            path.add(pathElement);
        }

        fieldItem = source.fieldItem;
    }

    ObjectPath(ObjectPath source, String fieldName) {
        this(source);
        PathElement pathElement = new PathElement();
        pathElement.type = PathElementType.field;
        pathElement.arrayIndex = -1;
        pathElement.fieldName = fieldName;
        path.add(pathElement);
    }

    ObjectPath(ObjectPath source, int index) {
        this(source);
        PathElement pathElement = new PathElement();
        pathElement.type = PathElementType.arrayElement;
        pathElement.arrayIndex = index;
        pathElement.fieldName = null;
        path.add(pathElement);
    }

    void setItem(PersistableItem item) {
        this.fieldItem = item;
    }

    void applyTo(PersistableItem item) throws PersistenceException, IllegalAccessException {
        Object lastObject = item;
        Iterator<PathElement> iterator = path.iterator();
        while (iterator.hasNext()) {
            PathElement pathElement = iterator.next();
            Class<?> clazz = lastObject.getClass();
            if (Map.class.isAssignableFrom(clazz)) {
                if (!iterator.hasNext()) {
                    ((Map) lastObject).put(pathElement.fieldName, fieldItem);
                } else {
                    throw new PersistenceException("Cannot find stuff again");
                }
            } else if (pathElement.type == PathElementType.field) {
                Field field = DefaultJsonSerialiser.findFieldInClass(clazz, pathElement.fieldName);
                field.setAccessible(true);
                Object currentObject = field.get(lastObject);
                if (currentObject == null) {
                    if (!iterator.hasNext()) {
                        field.set(lastObject, fieldItem);
                        return;
                    } else {
                        throw new PersistenceException("Cannot find stuff");
                    }
                }
                lastObject = currentObject;
            } else if (pathElement.type == PathElementType.arrayElement) {
                List<Object> list = (List<Object>) lastObject;
                Object currentObject = list.get(pathElement.arrayIndex);
                if (currentObject == null) {
                    if (!iterator.hasNext()) {
                        list.remove(pathElement.arrayIndex);
                        list.add(pathElement.arrayIndex, fieldItem);
                        return;
                    } else {
                        throw new PersistenceException("Cannot find stuff");
                    }
                }
                lastObject = currentObject;
            } else {
                throw new PersistenceException("Failed to find field type");
            }
        }
    }

    private enum PathElementType {
        field, arrayElement
    }

    private static class PathElement {
        PathElementType type;
        int arrayIndex;
        String fieldName;
    }
}
