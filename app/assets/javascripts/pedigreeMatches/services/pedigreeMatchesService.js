define([], function() {
'use strict';

	function PedigreeMatchesService(playRoutes, $q, userService, $http) {
        
		this.findMatches = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            console.log('FIND MATCHES');
            return $http.post('/pedigreeMatches/search', search);
            //return playRoutes.controllers.Pedigrees.findMatches().post(search);
		};

        this.findMatchesPedigree = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            console.log('FIND MATCHES PEDIGREE');
            return $http.post('/pedigree-matches-courtcase/search', search);
            //return playRoutes.controllers.Pedigrees.findMatchesPedigree().post(search);
        };

        this.countMatches = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            console.log('COUNT MATCHES');
            return $http.post('/pedigreeMatches/count', search);
            //return playRoutes.controllers.Pedigrees.countMatches().post(search);
        };

		this.getCourtCase = function(courtcaseId) {
			return playRoutes.controllers.Pedigrees.getCourtCaseFull(courtcaseId).get();
		};

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

        this.descarteMasivoByGroup = function (id, group) {
            console.log('MASSIVE DISCARD BY BROUP');
            return $http.post('/pedigreeMatches/masiveDiscardByGroup', id, group);
            //return playRoutes.controllers.Pedigrees.masiveDiscardByGroup(id, group).post();
        };

    }

	return PedigreeMatchesService;
});