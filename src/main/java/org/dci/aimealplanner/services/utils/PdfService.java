package org.dci.aimealplanner.services.utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.users.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;


@Service
@RequiredArgsConstructor
public class PdfService {
    private final SpringTemplateEngine templateEngine;

    public byte[] generatePdf(Recipe recipe, User author) {
        Context context = new Context();
        context.setVariable("recipe", recipe);
        context.setVariable("author", author);
        String renderedHtml = templateEngine.process("pdf/recipe", context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(renderedHtml, new ClassPathResource("").getURL().toExternalForm());
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}