package framework.servlet.binding;

import java.lang.reflect.Array;

final class ValueConverter {

    private ValueConverter() {
    }

    static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type.isArray()
                || type.isEnum()
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type;
    }

    static Object convertArray(String[] rawValues, Class<?> componentType) {
        Object array = Array.newInstance(componentType, rawValues.length);
        for (int i = 0; i < rawValues.length; i++) {
            Array.set(array, i, convertScalar(rawValues[i], componentType));
        }
        return array;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Object convertScalar(String rawValue, Class<?> targetType) {
        if (targetType == String.class) return rawValue;
        if (rawValue == null || rawValue.isBlank()) return defaultValueFor(targetType);
        if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(rawValue.trim());
        if (targetType == Long.class || targetType == long.class) return Long.parseLong(rawValue.trim());
        if (targetType == Double.class || targetType == double.class) return Double.parseDouble(rawValue.trim());
        if (targetType == Float.class || targetType == float.class) return Float.parseFloat(rawValue.trim());
        if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(rawValue.trim());
        if (targetType == Short.class || targetType == short.class) return Short.parseShort(rawValue.trim());
        if (targetType == Byte.class || targetType == byte.class) return Byte.parseByte(rawValue.trim());
        if (targetType == Character.class || targetType == char.class) return rawValue.charAt(0);
        if (targetType.isEnum()) return Enum.valueOf((Class<? extends Enum>) targetType, rawValue.trim());
        return rawValue;
    }

    static Object defaultValueFor(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
