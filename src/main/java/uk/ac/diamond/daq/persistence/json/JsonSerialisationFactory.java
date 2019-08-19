package uk.ac.diamond.daq.persistence.json;

import uk.ac.diamond.daq.persistence.service.impl.AbstractPersistenceService;

public interface JsonSerialisationFactory {
    JsonSerialiser getJsonSerialiser(AbstractPersistenceService persistenceService, String visitId);

    JsonDeserialiser getJsonDeserialiser(AbstractPersistenceService persistenceService, String visitId);
}
