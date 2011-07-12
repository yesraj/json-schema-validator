/*
 * Copyright (c) 2011, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eel.kitchen.jsonschema;

import eel.kitchen.jsonschema.validators.SchemaProvider;
import eel.kitchen.jsonschema.validators.Validator;
import eel.kitchen.util.IterableJsonNode;
import org.codehaus.jackson.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class JasonSchema
{
    private final ValidatorFactory factory = new ValidatorFactory();
    private final List<String> validationErrors = new LinkedList<String>();
    private final JsonNode schema;

    public JasonSchema(final JsonNode schema)
    {
        this.schema = schema;
    }

    public boolean validate(final JsonNode node)
    {
        validationErrors.clear();

        final List<String> messages = validateOneNode(schema, node, "$");

        if (messages.isEmpty())
            return true;

        validationErrors.addAll(messages);
        return false;
    }

    public List<String> getValidationErrors()
    {
        return Collections.unmodifiableList(validationErrors);
    }

    private List<String> validateOneNode(final JsonNode schema,
        final JsonNode node, final String path)
    {
        final Validator v = factory.getValidator(schema, node);
        final SchemaProvider schemaProvider = v.getSchemaProvider();
        final IterableJsonNode inode = new IterableJsonNode(node);

        final List<String> messages = new ArrayList<String>();

        if (!v.validate(node)) {
            for (final String message: v.getValidationErrors())
                messages.add(String.format("%s: %s", path, message));
            return messages;
        }

        String fullpath, subpath;
        JsonNode subschema, subnode;
        List<String> submessages;

        for (final Map.Entry<String, JsonNode> entry: inode) {
            subpath = entry.getKey();
            subnode = entry.getValue();
            fullpath = String.format("%s.%s", path, subpath);
            subschema = schemaProvider.getSchemaForPath(subpath);
            submessages = validateOneNode(subschema, subnode, fullpath);
            messages.addAll(submessages);
        }

        return messages;
    }
}
