<#import "base.ftl" as base/>
<@base.page>
<div class="row">
    <div class="large-8 columns">
        <#if employee.id == 0>
        <#-- New employee -->
            <h2>${i18n('fathom.newEmployee')}</h2>
        <#else>
        <#-- Update employee -->
            <h2>${i18n('fathom.employee')}
                <small>#${employee.id}</small>
            </h2>
        </#if>

        <form method="POST">
            <div class="row">
                <div class="large-6 columns">
                    <label>${i18n('fathom.name')}
                        <input type="text" name="name" value="${(employee.name)!}" autofocus="true">
                    </label>
                </div>
                <div class="large-6 columns">
                    <label>${i18n('fathom.office')}
                        <select name="office">
                            <#list offices as office>
                                <#if office == (employee.office)!>
                                    <option selected value="${office}">${office}</option>
                                <#else>
                                    <option value="${office}">${office}</option>
                                </#if>

                            </#list>
                        </select>
                    </label>
                </div>
            </div>
            <div class="row">
                <div class="large-6 columns">
                    <label>${i18n('fathom.position')}
                        <select name="position">
                            <#list positions as position>
                                <#if position == (employee.position)!>
                                    <option selected value="${position}">${position}</option>
                                <#else>
                                    <option value="${position}">${position}</option>
                                </#if>

                            </#list>
                        </select>
                    </label>
                </div>

                <div class="large-6 columns">
                    <label>${i18n('fathom.salary')}
                        <input type="text" name="salary" value="${(employee.salary)!}">
                    </label>
                </div>
            </div>
            <div class="row">
                <div class="large-6 columns">
                    <label>${i18n('fathom.extension')}
                        <input type="text" name="extension" value="${(employee.extension)!}">
                    </label>
                </div>
                <div class="large-6 columns">
                    <label>${i18n('fathom.startDate')}
                        <input type="text" name="startDate" class="datepicker"
                               data-value="${employee.startDate!?string("yyyy-MM-dd")}">
                    </label>
                </div>
            </div>
            <div class="row">
                <div class="large-12 columns">
                    <input type="hidden" name="_csrf_token" value="${csrfToken}">
                    <button type="submit" class="small radius">${i18n('fathom.save')}</button>
                    <a class="button small radius secondary"
                       href="${appPath}/secure/employees">${i18n('fathom.cancel')}</a>
                </div>
            </div>
        </form>
    </div>
    <div class="large-4 columns">
    <#-- Only show Delete button for employee updates -->
        <#if employee.id != 0>
            <button class="small radius alert" href="#"
                    data-reveal-id="deleteEmployeeModal">${i18n('fathom.delete')}</button>

            <div id="deleteEmployeeModal" class="reveal-modal" data-reveal>
                <h2>${employee.name}</h2>

                <p class="lead">Are you sure you want to delete this employee?</p>
                <a class="close-reveal-modal">&#215;</a>

                <form class="inline" method="POST" action="${appPath}/secure/employee/${employee.id}/delete">
                    <input type="hidden" name="_csrf_token" value="${csrfToken}"/>
                    <button type="submit" class="small radius alert">
                    ${i18n('fathom.delete')}
                    </button>
                </form>
            </div>

        </#if>
    </div>
</div>
</@base.page>