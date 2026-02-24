<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>

    <#if section = "header">
        <h1 class="login-page-title">Erro</h1>
        <p class="login-subtitle">Ocorreu um problema</p>
    </#if>

    <#if section = "form">
        <div class="alert alert-error">
            ${kcSanitize(message.summary)?no_esc}
        </div>

        <#if skipLink??>
        <#else>
            <#if client?? && client.baseUrl?has_content>
                <a href="${client.baseUrl}" class="btn-brand" style="text-align:center;text-decoration:none;display:block;">
                    Voltar ao aplicativo
                </a>
            </#if>
        </#if>

        <a href="${url.loginUrl}" class="back-link">&larr; Voltar ao login</a>
    </#if>

</@layout.registrationLayout>
