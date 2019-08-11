package com.sun.sso.server;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class SsoSpelView implements View {
    private final String template;
    private final String prefix;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final StandardEvaluationContext context = new StandardEvaluationContext();
    private PlaceholderResolver resolver;

    public SsoSpelView(String template) {
        this.template = template;
        this.prefix = (new RandomValueStringGenerator()).generate() + "{";
        this.context.addPropertyAccessor(new MapAccessor());
        this.resolver = new PlaceholderResolver() {
            public String resolvePlaceholder(String name) {
                Expression expression = SsoSpelView.this.parser.parseExpression(name);
                Object value = expression.getValue(SsoSpelView.this.context);
                return value == null ? null : value.toString();
            }
        };
    }

    public String getContentType() {
        return "text/html";
    }

    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap(model);
        String path = ServletUriComponentsBuilder.fromContextPath(request).build().getPath();
        map.put("path", path == null ? "" : path);
        this.context.setRootObject(map);
        String maskedTemplate = this.template.replace("${", this.prefix);
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(this.prefix, "}");
        String result = helper.replacePlaceholders(maskedTemplate, this.resolver);
        result = result.replace(this.prefix, "${");
        response.setContentType(this.getContentType());
        response.getWriter().append(result);
    }
}