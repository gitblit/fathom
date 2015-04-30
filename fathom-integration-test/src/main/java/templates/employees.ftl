<#import "base.ftl" as base/>
<#import "alerts.ftl" as alerts/>
<@base.page>
<div class="row">
    <div class="large-11 columns">
        <h2>${i18n('fathom.employees')}</h2>
    </div>
    <div class="large-1 columns">
        <ul class="button-group even-1">
            <li><a class="button tiny radius" href="${appPath}/secure/employee/0"
                   title="${i18n('fathom.add')}"><i
                    class="fa fa-plus"></i></a></li>
        </ul>
    </div>
</div>

<div class="row">
    <div class="large-6 columns large-offset-3">
        <@alerts.list messages=flash.getErrorList() type="alert"/>
        <@alerts.list messages=flash.getWarningList() type="warning"/>
        <@alerts.list messages=flash.getInfoList() type="info"/>
        <@alerts.list messages=flash.getSuccessList() type="success"/>
    </div>
</div>

<div class="row">
    <div class="large-12 columns">
        <table id="employees" class="display" width="100%">
            <thead>
            <tr>
                <th>${i18n('fathom.name')}</th>
                <th>${i18n('fathom.position')}</th>
                <th>${i18n('fathom.office')}</th>
                <th>${i18n('fathom.extension')}</th>
                <#if account.isAdministrator()>
                    <th>${i18n('fathom.startDate')}</th>
                    <th>${i18n('fathom.salary')}</th>
                    <th data-orderable="false"></th>
                </#if>
            </tr>
            </thead>
            <tbody>
                <#list employees as employee>
                <tr>
                    <td>${employee.name}</td>
                    <td>${employee.position!}</td>
                    <td>${employee.office!}</td>
                    <td>${employee.extension!}</td>
                    <#if account.isAdministrator()>
                        <td>${employee.startDate?date}</td>
                        <td>${employee.salary!}</td>
                        <td style="text-align: right;">
                            <div class="inline">
                                <a class="button tiny radius secondary"
                                   title="${i18n('fathom.edit')}"
                                   href="${appPath}/secure/employee/${employee.id}"><i
                                        class="fa fa-pencil"></i></a>
                            </div>
                        </td>
                    </#if>
                </tr>
                </#list>
            </tbody>
        </table>
    </div>
</div>
</@base.page>
