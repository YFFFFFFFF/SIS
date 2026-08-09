package com.sis.iids.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI iidsOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("智能投资测算与决策支持系统 API")
                        .version("0.2.0")
                        .description("M1→PRD 升级（R-02 财务引擎 v2）API 契约，覆盖项目、方案、测算、投资/成本分项、三类报表、利润流向、还本付息、报表、审批和审计流程。"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OpenApiCustomizer publicLoginOperationCustomizer() {
        return openApi -> {
            PathItem loginPath = openApi.getPaths().get("/api/v1/auth/login");
            if (loginPath != null && loginPath.getPost() != null) {
                loginPath.getPost().setSecurity(List.of());
            }
        };
    }
}