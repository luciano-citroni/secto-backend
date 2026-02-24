<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>

    <#if section = "header">
        <h1 class="login-page-title">Esqueceu a senha?</h1>
        <p class="login-subtitle">Informe seu usuário ou email para redefinir sua senha</p>
    </#if>

    <#if section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">

            <div class="form-group">
                <label for="username">
                    <#if !realm.loginWithEmailAllowed>${msg("username")}
                    <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                    <#else>${msg("email")}</#if>
                </label>
                <input
                    type="text"
                    id="username"
                    name="username"
                    autofocus
                    autocomplete="off"
                    placeholder="seu.usuario ou email@exemplo.com"
                    aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                />
                <#if messagesPerField.existsError('username')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}
                    </span>
                </#if>
            </div>

            <button type="submit" class="btn-brand">Enviar</button>
        </form>

        <a href="${url.loginUrl}" class="back-link">&larr; Voltar ao login</a>
    </#if>

    <#if section = "info">
        <p>${msg("emailInstruction")}</p>
    </#if>

</@layout.registrationLayout>
