package uk.ac.diamond.daq.persistence.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigInteger;

public class PersistenceIdSerializer extends StdSerializer<BigInteger> {
    public PersistenceIdSerializer() {
        super(BigInteger.class);
    }

    @Override
    public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString(16));
    }
}

