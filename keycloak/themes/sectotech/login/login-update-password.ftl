<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>

    <#if section = "header">
        <h1 class="login-page-title">Atualizar Senha</h1>
        <p class="login-subtitle">Defina uma nova senha para sua conta</p>
    </#if>

    <#if section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">

            <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly="readonly" style="display:none;"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>

            <div class="form-group">
                <label for="password-new">Nova Senha</label>
                <input
                    type="password"
                    id="password-new"
                    name="password-new"
                    autofocus
                    autocomplete="new-password"
                    placeholder="••••••••"
                    aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                />
                <#if messagesPerField.existsError('password')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="form-group">
                <label for="password-confirm">Confirmar Nova Senha</label>
                <input
                    type="password"
                    id="password-confirm"
                    name="password-confirm"
                    autocomplete="new-password"
                    placeholder="••••••••"
                    aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                />
                <#if messagesPerField.existsError('password-confirm')>
                    <span class="alert alert-error" style="margin-top:0.25rem;display:block;padding:0.5rem 0.75rem;">
                        ${kcSanitize(messagesPerField.getFirstError('password-confirm'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="form-actions" style="justify-content: flex-end;">
                <#if isAppInitiatedAction??>
                    <button type="submit" class="btn-brand" style="width:auto;flex:1;">Salvar</button>
                    <button type="submit" class="btn-link" name="cancel-aia" value="true" style="width:auto;">Cancelar</button>
                <#else>
                    <button type="submit" class="btn-brand">Salvar</button>
                </#if>
            </div>
        </form>
    </#if>

</@layout.registrationLayout>
