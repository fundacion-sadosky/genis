define([],function() {
    'use strict';

    function AnalysisTypeService(playRoutes, $q) {

        this.list = function() {
            return playRoutes.controllers.AnalysisTypes.list().get();
        };
        
        this.listById = function() {
            var deferred = $q.defer();
            
            this.list().then(function(response) {
                var analysisTypes = response.data;
                var analysisTypesById = {};
                analysisTypes.forEach(function(at) {
                    analysisTypesById[at.id] = at;
                });
                deferred.resolve(analysisTypesById);
            }, function(error) {
                deferred.reject(error);
            });
            
            return deferred.promise;

        };

    }
        
    return AnalysisTypeService;

});