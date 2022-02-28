package nu.fgv.register.server.util.export;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
class FieldAccessor {

    protected final Field field;

    public FieldAccessor(final Field field) {
        this.field = field;
    }

    public Object getObject(final Object obj) {
        try {
            return field.get(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return null;
        }
    }

    public Integer getInt(final Object obj) {
        try {
            return field.getType().equals(Integer.class) ? (Integer) field.get(obj) : field.getInt(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return 0;
        }
    }

    public Float getFloat(final Object obj) {
        try {
            return field.getType().equals(Float.class) ? (Float) field.get(obj) : field.getFloat(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return 0f;
        }
    }

    public Double getDouble(final Object obj) {
        try {
            return field.getType().equals(Double.class) ? (Double) field.get(obj) : field.getDouble(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return 0d;
        }
    }

    public Long getLong(final Object obj) {
        try {
            return field.getType().equals(Long.class) ? (Long) field.get(obj) : field.getLong(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return 0L;
        }
    }

    public Short getShort(final Object obj) {
        try {
            return field.getType().equals(Short.class) ? (Short) field.get(obj) : field.getShort(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return 0;
        }
    }

    public Byte getByte(final Object obj) {
        try {
            return field.getType().equals(Byte.class) ? (Byte) field.get(obj) : field.getByte(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return 0;
        }
    }

    public char getChar(final Object obj) {
        try {
            return field.getChar(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return ' ';
        }
    }

    public Boolean getBoolean(final Object obj) {
        try {
            return field.getType().equals(Boolean.class) ? (Boolean) field.get(obj) : field.getBoolean(obj);
        } catch (final IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            log.warn("Unable to read field from object", e);
            return false;
        }
    }

}
