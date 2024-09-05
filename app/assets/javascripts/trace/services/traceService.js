define([ 'angular' ], function() {
    'use strict';

    function TraceService(playRoutes, $http) {

        this.search = function(search) {
            console.log('TRACES SEARCH');
            return $http.post('/trace/search', search);
            //return playRoutes.controllers.Traces.search().post(search);
        };

        this.count = function(search) {
            console.log('TRACES COUNT');
            return $http.post('/trace/total', search);
            //return playRoutes.controllers.Traces.count().post(search);
        };
        this.searchPedigree = function(search) {
            console.log('TRACES SEARCH PEDIGREE');
            return $http.post('/trace/search-pedigree', search);
            //return playRoutes.controllers.Traces.searchPedigree().post(search);
        };

        this.countPedigree = function(search) {
            console.log('TRACES COUNT PEDIGREE');
            return $http.post('/trace/total-pedigree', search);
            //return playRoutes.controllers.Traces.countPedigree().post(search);
        };
        this.getFullDescription = function(id) {
            console.log('GET FULL DESCRIPTION');
            return $http.get('/trace/full-description/' + id);
            //return playRoutes.controllers.Traces.getFullDescription(id).get();
        };

    }

    return TraceService;

});