define([], function() {
'use strict';

	function PedigreeMatchesGroupsService(playRoutes, userService) {

		this.getCourtCase = function(courtcaseId) {
			return playRoutes.controllers.Pedigrees.getCourtCaseFull(courtcaseId).get();
		};

        this.getPedigree = function(pedigreeId) {
            return playRoutes.controllers.Pedigrees.getPedigree(pedigreeId).get();
        };
        
        this.getPedigreeCoincidencia = function (id) {
            return playRoutes.controllers.Pedigrees.getPedigreeCoincidencia(id).get();
        };

        this.findByCodeCoincidencia = function (globalCodes) {
            return playRoutes.controllers.Pedigrees.getMatchByProfile(globalCodes).get();
        };

        this.getMatchesByGroup = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            return playRoutes.controllers.Pedigrees.getMatchesByGroup().post(search);
        };

        this.countMatchesByGroup = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            return playRoutes.controllers.Pedigrees.countMatchesByGroup().post(search);
        };

        this.exportMatchesByGroup = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            return playRoutes.controllers.Pedigrees.exportMatchesByGroup().post(search);
        };

        this.discard = function(oid) {
          return playRoutes.controllers.Pedigrees.discard(oid).post();  
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