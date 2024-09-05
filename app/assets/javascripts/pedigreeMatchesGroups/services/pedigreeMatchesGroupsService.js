define([], function() {
'use strict';

	function PedigreeMatchesGroupsService(playRoutes, userService, $http) {

		this.getCourtCase = function(courtcaseId) {
            return $http.get('/pedigree/full/' + courtcaseId);
			//return playRoutes.controllers.Pedigrees.getCourtCaseFull(courtcaseId).get();
		};

        this.getPedigree = function(pedigreeId) {
            return $http.get('/pedigree/genogram/' + pedigreeId);
            //return playRoutes.controllers.Pedigrees.getPedigree(pedigreeId).get();
        };
        
        this.getPedigreeCoincidencia = function (id) {
            return $http.get('/pedigree/pedigree-coincidencia', { params: { id: id } });
            //return playRoutes.controllers.Pedigrees.getPedigreeCoincidencia(id).get();
        };

        this.findByCodeCoincidencia = function (globalCodes) {
            return $http.get('/pedigree/perfil-coincidencia', { params: { globalCodes: globalCodes } });
            //return playRoutes.controllers.Pedigrees.getMatchByProfile(globalCodes).get();
        };

        this.getMatchesByGroup = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            console.log('GET MATCHES BY GROUP');
            return $http.post('/pedigreeMatches/groups/search', search);
            //return playRoutes.controllers.Pedigrees.getMatchesByGroup().post(search);
        };

        this.countMatchesByGroup = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            console.log('COUNT MATCHES BY GROUP');
            return $http.post('/pedigreeMatches/groups/count', search);
            //return playRoutes.controllers.Pedigrees.countMatchesByGroup().post(search);
        };

        this.exportMatchesByGroup = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            console.log('EXPORT MATCHES BY GROUP');
            return $http.post('/pedigreeMatches/groups/export', search);
            //return playRoutes.controllers.Pedigrees.exportMatchesByGroup().post(search);
        };

        this.discard = function(oid) {
            console.log('DISCARD');
            return $http.post('/pedigree/discard', null, { params: { matchId: oid } });
            //return playRoutes.controllers.Pedigrees.discard(oid).post();  
        };
        
        this.canDiscard = function(match, user) {
            return (match.pedigree.status !== 'discarded') &&
                    ((match.pedigree.assignee === user.name) ||
                    user.superuser);
        };

        this.canDiscardCourtCase = function(match, user) {
            return match.estado !== 'discarded' &&
                ((match.assigne === user.name) ||
                    user.superuser);
        };

        this.isHit = function (match) {
            return (match.pedigree.status === 'hit');
        };

        this.isHitCourtCase = function (match) {
            return (match.estado === 'hit');
        };
        
	}

	return PedigreeMatchesGroupsService;
});