package io.lnk.protocol.jackson;

import io.lnk.api.ProtocolVersion;
import io.lnk.protocol.BasicProtocolFactory;
import io.lnk.protocol.Serializer;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:46:42
 */
public class JacksonProtocolFactory extends BasicProtocolFactory {
    private final Serializer serializer;
    
    public JacksonProtocolFactory() {
        super(ProtocolVersion.DEFAULT_PROTOCOL);
        this.serializer = new JacksonSerializer();
    }

    @Override
    public byte[] encode(Object obj) {
        return serializer.serializeAsBytes(obj);
    }

    @Override
    public <T> T decode(Class<T> objType, byte[] data) {
        return serializer.deserialize(objType, data);
    }
}
