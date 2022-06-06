define([ 'angular' ], function() {
    'use strict';

    function TraceService(playRoutes) {

        this.search = function(search) {
            return playRoutes.controllers.Traces.search().post(search);
        };

        this.count = function(search) {
            return playRoutes.controllers.Traces.count().post(search);
        };
        this.searchPedigree = function(search) {
            return playRoutes.controllers.Traces.searchPedigree().post(search);
        };

        this.countPedigree = function(search) {
            return playRoutes.controllers.Traces.countPedigree().post(search);
        };
        this.getFullDescription = function(id) {
            return playRoutes.controllers.Traces.getFullDescription(id).get();
        };

    }

    return TraceService;

});