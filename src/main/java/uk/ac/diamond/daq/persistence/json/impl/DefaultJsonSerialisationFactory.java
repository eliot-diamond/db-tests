package uk.ac.diamond.daq.persistence.json.impl;

import uk.ac.diamond.daq.persistence.json.JsonDeserialiser;
import uk.ac.diamond.daq.persistence.json.JsonSerialisationFactory;
import uk.ac.diamond.daq.persistence.json.JsonSerialiser;
import uk.ac.diamond.daq.persistence.service.impl.AbstractPersistenceService;

public class DefaultJsonSerialisationFactory implements JsonSerialisationFactory {
    @Override
    public JsonSerialiser getJsonSerialiser(AbstractPersistenceService persistenceService, String visitId) {
        return new DefaultJsonSerialiser(persistenceService, visitId);
    }

    @Override
    public JsonDeserialiser getJsonDeserialiser(AbstractPersistenceService persistenceService, String visitId) {
        return new DefaultJsonDeserialiser(persistenceService, visitId);
    }
}
