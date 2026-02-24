<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=social.displayInfo; section>

    <#if section = "header">
        <h1 class="login-page-title">Entrar</h1>
        <p class="login-subtitle">Acesse sua conta para continuar</p>
    </#if>

    <#if section = "form">

        <#-- Social Providers -->
        <#if realm.password && social.providers??>
            <div class="social-providers">
                <#list social.providers as p>
                    <a href="${p.loginUrl}" class="social-btn" id="social-${p.alias}">
                        <#if p.iconClasses?has_content>
                            <i class="${p.iconClasses!}"></i>
                        </#if>
                        <span>${p.displayName!}</span>
                    </a>
                </#list>
            </div>
            <div class="divider">ou</div>
        </#if>

        <#if realm.password>
            <form id="kc-form-login" action="${url.loginAction}" method="post">

                <div class="form-group">
                    <label for="username">
                        <#if !realm.loginWithEmailAllowed>${msg("username")}
                        <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                        <#else>${msg("email")}</#if>
                    </label>
                    <input
                        tabindex="1"
                        id="username"
                        name="username"
                        value="${(login.username!'')}"
                        type="text"
                        autofocus
                        autocomplete="off"
                        placeholder="seu.usuario ou email@exemplo.com"
                        aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                    />
                    <#if messagesPerField.existsError('username','password')>
                        <span class="alert alert-error" style="margin-top:0.25rem;display:block;">
                            ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                        </span>
                    </#if>
                </div>

                <div class="form-group">
                    <label for="password">${msg("password")}</label>
                    <input
                        tabindex="2"
                        id="password"
                        name="password"
                        type="password"
                        autocomplete="off"
                        placeholder="••••••••"
                        aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                    />
                </div>

                <div class="form-actions">
                    <#if realm.rememberMe && !usernameEditDisabled??>
                        <div class="checkbox-group">
                            <input
                                tabindex="3"
                                id="rememberMe"
                                name="rememberMe"
                                type="checkbox"
                                <#if login.rememberMe??>checked</#if>
                            />
                            <label for="rememberMe">${msg("rememberMe")}</label>
                        </div>
                    <#else>
                        <div></div>
                    </#if>

                    <#if realm.resetPasswordAllowed>
                        <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="forgot-password">
                            ${msg("doForgotPassword")}
                        </a>
                    </#if>
                </div>

                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

                <button tabindex="4" type="submit" class="btn-brand">
                    ${msg("doLogIn")}
                </button>
            </form>
        </#if>

        <#-- Link para registro -->
        <div class="login-footer">
            <p>Não possui uma conta ainda? <a href="https://sectotech.wearebridge.com.br/registrar">Registre-se</a></p>
        </div>

    </#if>

</@layout.registrationLayout>
