package org.moshang.tempusetchaos.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ChrononCableGeometryLoader implements IGeometryLoader<ChrononCableGeometry> {
    @Override
    @NotNull
    public ChrononCableGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        return new ChrononCableGeometry();
    }
}
