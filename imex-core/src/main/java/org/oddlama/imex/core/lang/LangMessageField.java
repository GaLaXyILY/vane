package org.oddlama.imex.core.lang;

import org.oddlama.imex.core.YamlLoadException;
import org.oddlama.imex.core.Module;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.apache.commons.lang.WordUtils;
import java.text.MessageFormat;

import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;

public class LangMessageField extends LangField<MessageFormat> {
	public LangMessage annotation;

	public LangMessageField(Module module, Field field, LangMessage annotation) {
		super(module, field, MessageFormat.class);
		this.annotation = annotation;
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isString(get_yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected string");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.set(module, new MessageFormat(yaml.getString(get_yaml_path())));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

