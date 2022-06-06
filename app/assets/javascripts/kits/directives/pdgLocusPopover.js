define([], function() {
'use strict';

function pdgLocusPopover($compile, locusService) {

    return {
        restrict: 'A',
        scope: {
            locus: '='
        },
        link: function(scope, el) {

            el.on('$destroy', function() {
                el.popover('destroy');
            });

            locusService.list().then(function(response) {
                scope.locusById = {};
                response.data.forEach(function(l) {
                    scope.locusById[l.id] = l;
                });
                helpTipScope.locusById = scope.locusById;
            });

            var helpTipScope = scope.$new(true);
            helpTipScope.locus = scope.locus;
            var table = '<table class="locus-plain" style="width:100%;"><tr ng:repeat="l in locus" style="border: 2px solid #FFFFFF" class="fl-{{l.fluorophore.toLowerCase()}}"><td>{{locusById[l.locus].name}}</td></tr></tbody></table>';
            var helpTipContent = $compile(table)(helpTipScope);

            el.popover({
                content : helpTipContent,
                html : true,
                trigger: 'focus',
                placement : 'left'});
        }
    };
}

return pdgLocusPopover;
});