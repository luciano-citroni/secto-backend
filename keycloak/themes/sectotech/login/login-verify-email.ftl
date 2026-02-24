<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>

    <#if section = "header">
        <h1 class="login-page-title">Verificar Email</h1>
        <p class="login-subtitle">Enviamos um email para verificar seu endereço</p>
    </#if>

    <#if section = "form">
        <p style="font-size: 0.875rem; color: var(--muted-foreground); margin-bottom: 1.5rem; text-align:center;">
            ${msg("emailVerifyInstruction1")}
        </p>

        <p style="font-size: 0.875rem; color: var(--muted-foreground); margin-bottom: 1rem; text-align:center;">
            ${msg("emailVerifyInstruction2")}
            <br/>
            <a href="${url.loginAction}" style="font-weight:500;">${msg("doClickHere")}</a>
            ${msg("emailVerifyInstruction3")}
        </p>
    </#if>

</@layout.registrationLayout>
