package uk.ac.diamond.daq.persistence.logging.impl;

import uk.ac.diamond.daq.persistence.data.PersistableItem;
import uk.ac.diamond.daq.persistence.service.PersistenceException;

import java.io.*;

public class ItemContainer {
    private byte[] data;

    private long id;

    private long version;

    public ItemContainer(PersistableItem item) throws PersistenceException {
        this.id = item.getId();
        this.version = item.getVersion();

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(item);
            data = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new PersistenceException("Cannot save item", e);
        }
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public PersistableItem getItem() throws PersistenceException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (PersistableItem) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException("Unable to deserialize object", e);
        }
    }
}
