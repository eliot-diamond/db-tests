package uk.ac.diamond.daq.persistence.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigInteger;

public class PersistenceIdDeserializer extends StdDeserializer<BigInteger> {
    public PersistenceIdDeserializer() {
        super(BigInteger.class);
    }

    @Override
    public BigInteger deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return new BigInteger(p.getText());
    }
}
