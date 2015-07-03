<#-------------------------------------------------------
    login.ftl

    This Freemarker template demonstrates:
      1. template inheritance
      2. variable binding
      3. localized messages
      4. condition statements

    Freemarker documentation
    http://freemarker.org

-------------------------------------------------------->
<#import "base.ftl" as base/>
<@base.page>
<div class="row">
    <div class="large-12 columns" style="text-align: center;padding-top: 20px;">
        <div><i style="color:gray; font-size: 4em;" class="fa fa-anchor"></i></div>
        <h1>${i18n('fathom.pleaseSignIn')}</h1>
    </div>
</div>

<div class="row">
    <div class="large-4 columns large-offset-4" style="text-align: center;">
        <form accept-charset="UTF-8" method="post">
            <#if flash.hasError()>
                <small class="error">
                ${flash.getError()}
                </small>
            </#if>
            <div class="row">
                <input autofocus="true" name="username" type="text" placeholder="${i18n('fathom.username')}">
            </div>
            <div class="row">
                <input name="password" type="password" placeholder="${i18n('fathom.password')}">
            </div>
            <div class="row">
                <label>
                    <input name="rememberMe" type="checkbox" value="true"> ${i18n('fathom.rememberMe')}
                </label>
            </div>
            <input class="button" type="submit" value="${i18n('fathom.login')}">
        </form>
    </div>
</div>
</@base.page>