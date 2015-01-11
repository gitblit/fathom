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

<div class="row">
    <div class="large-6 columns large-offset-3" style="padding-top:20px;">
        <div class="panel">
            <h4>Test Accounts</h4>
            <table role="grid" width="100%">
                <thead>
                <tr>
                    <th>
                    ${i18n('fathom.username')}</td>
                    <td>${i18n('fathom.password')}</td>
                    <th>Permission</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>admin</td>
                    <td>admin</td>
                    <td>authorized</td>
                </tr>
                <tr>
                    <td>user</td>
                    <td>user</td>
                    <td>disabled</td>
                </tr>
                <tr>
                    <td>guest</td>
                    <td>guest</td>
                    <td>forbidden</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
</div>
</@base.page>