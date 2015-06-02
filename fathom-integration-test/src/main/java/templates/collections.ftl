<#import "base.ftl" as base/>
<@base.page>
<div class="row">
    <div class="large-12 columns">
        <h2>Collections
            <small>POST[PUT] indexed Sets, Lists, & TreeSets from a form</small>
        </h2>
    </div>
</div>
<div class="row">
    <div class="large-9 columns">
        <form method="POST">
            <div class="row">
                <div class="large-4 columns">
                    <h4>Set&lt;Integer&gt;</h4>
                    <#list mySet as value>
                        <label>${value_index}
                            <input type="text" id="mySet[${value_index}]" name="mySet[${value_index}]" value="${value}">
                        </label>
                    </#list>
                </div>
                <div class="large-4 columns">
                    <h4>List&lt;Integer&gt;</h4>
                    <#list myList as value>
                        <label>${value_index}
                            <input type="text" id="myList[${value_index}]" name="myList[${value_index}]"
                                   value="${value}">
                        </label>
                    </#list>
                </div>
                <div class="large-4 columns">
                    <h4>TreeSet&lt;String&gt;</h4>
                    <#list myTreeSet as value>
                        <label>${value_index}
                            <input type="text" id="myTreeSet[${value_index}]" name="myTreeSet[${value_index}]"
                                   value="${value}">
                        </label>
                    </#list>
                </div>
            </div>

            <div class="row">
                <div class="large-12 columns">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="_method" value="PUT">
                    <button type="submit" class="small radius">${i18n('fathom.save')}</button>
                    <a class="button small radius secondary" href="${appPath}/">${i18n('fathom.cancel')}</a>
                </div>
            </div>
        </form>
    </div>
</div>
</@base.page>