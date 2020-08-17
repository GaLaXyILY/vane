package org.oddlama.imex.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.core.Module;
import org.oddlama.imex.core.YamlLoadException;

public class ConfigLongField extends ConfigField<Long> {
	public ConfigLong annotation;

	public ConfigLongField(Module module, Field field, ConfigLong annotation) {
		super(module, field, "long");
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		append_description(builder, annotation.desc());
		append_value_range(builder, annotation.min(), annotation.max(), Long.MIN_VALUE, Long.MAX_VALUE);
		append_default_value(builder, annotation.def());
		append_field_definition(builder, annotation.def());
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!(yaml.get(get_yaml_path()) instanceof Number)) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected long");
		}

		var val = yaml.getLong(get_yaml_path());
		if (annotation.min() != Long.MIN_VALUE && val < annotation.min()) {
			throw new YamlLoadException("Configuration '" + get_yaml_path() + "' has an invalid value: Value must be >= " + annotation.min());
		}
		if (annotation.max() != Long.MAX_VALUE && val > annotation.max()) {
			throw new YamlLoadException("Configuration '" + get_yaml_path() + "' has an invalid value: Value must be <= " + annotation.max());
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setLong(module, yaml.getLong(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

