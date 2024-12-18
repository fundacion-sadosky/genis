define([], function() {
'use strict';

	function MatchesService(playRoutes, userService, $http) {
		
		this.doHit = function(matchingId, firingCode){
			console.log('CONVERT HIT');
			return $http.post('/convertHit', matchingId, firingCode);
			//return playRoutes.controllers.Matching.convertHit(matchingId, firingCode).post();
		};
		
		this.doDiscard = function(matchingId, firingCode){
			console.log('CONVERT DISCARD');
			return $http.post('/convertDiscard', matchingId, firingCode);
			//return playRoutes.controllers.Matching.convertDiscard(matchingId, firingCode).post();
		};

		this.doUpload = function(matchingId, firingCode){
			console.log('UPLOAD STATUS');
			return $http.post('/uploadStatus', matchingId, firingCode);
			//return playRoutes.controllers.Matching.uploadStatus(matchingId, firingCode).post();
		};

		this.canUpload = function(matchingId){
			return $http.get('/canUploadMatchStatus?matchingId='+matchingId);
			//return playRoutes.controllers.Matching.canUploadMatchStatus(matchingId).get();
		};
		
		this.findMatches = function(matchingId) {
			return $http.get('/matching?matchingId='+matchingId)
			.then(function(response) {
				var res = response.data;
				fillReducedStringencies(res.results);
				return res;
			})
			.catch(function(error) {
				console.error('Error during findMatches:', error);
			});
			//return playRoutes.controllers.Matching.findMatchesByCode(matchingId).get()
			//.success(function(data/*, status, headers, response*/) {
			//var res = data;
			//fillReducedStringencies(res.results);
			//data = res;
			//}).error(function() {
			////$log.info('Matching.findByCode :(');
			//});
		};

		this.getTotalMatchesByGroup = function (search) {
			var user = userService.getUser();
			search.user = user.name;
			search.isSuperUser = user.superuser;
			console.log('GET TOTAL MATCHES BY GROUP');
			return $http.post('/user-total-matches-group', search);
			//return playRoutes.controllers.Matching.getTotalMatchesByGroup().post(search);
		};

        this.searchMatchesProfile = function (globalCode) {
			return $http.get('/matching-profile?globalCode=',globalCode);
			//return playRoutes.controllers.Matching.searchMatchesProfile(globalCode).get();
		};

		this.getMatchesByGroup = function (search) {
			var user = userService.getUser();
			search.user = user.name;
			search.isSuperUser = user.superuser;
			console.log('GET MATCHES BY GROUP');
			return $http.post('/user-matches-group', search);
			//return playRoutes.controllers.Matching.getMatchesByGroup().post(search);
		};

		this.searchMatches = function(search){
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
			console.log('GET MATCHES');
			return $http.post('/user-matches', search);
			//return playRoutes.controllers.Matching.getMatches().post(search);
		};

		this.getTotalMatches = function(search){
			var user = userService.getUser();
			search.user = user.name;
			search.isSuperUser = user.superuser;
			console.log('GET TOTAL MATCHES');
			return $http.post('/user-total-matches', search);
			//return playRoutes.controllers.Matching.getTotalMatches().post(search);
		};

		this.getResults = function(
			matchingId,
			isPedigreeMatch,
			isCollapsing,
			isScreening
		) {
			return $http.get("/getByMatchedProfileId?matchingId="+matchingId+"&isPedigreeMatch="+isPedigreeMatch+"&isCollapsing="+isCollapsing+"&isScreening="+isScreening)
			.then(function(response) {
				var data = response.data;
				if (data && data.results) {
					fillReducedStringencies(data.results);
				}
				return data;
			})
			.catch(function(error) {
				console.error('Error during getResults:', error);
			});
			//return playRoutes.controllers.Matching.getByMatchedProfileId(
			//matchingId,
			//isPedigreeMatch,
			//isCollapsing,
			//isScreening
			//).get()
			//.success(
			//function(data/*, status, headers, response*/) {
			//if (data && data.results) {
			//fillReducedStringencies(data.results);
			//}
			//}).error(function() {
			////$log.info('Matching.getByMatchedProfileId :(');
			//});
		};
		
		this.getComparedGenotyfications = function(leftGlobalCode, rightGlobalCode,matchId,isCollapsing,isScreening) {
			return this.getComparedMixtureGene([leftGlobalCode, rightGlobalCode],matchId,isCollapsing,isScreening);
		};
		
		this.getLR = function(profileId, matchedProfileId, matchingId, selectedOptions){
			// TODO: calcular a partir del matchingId
            var lrRequest = {'firingCode': profileId, 'matchingCode': matchedProfileId, 'stats': selectedOptions,'matchingId':matchingId};
			console.log('GET LR');
			return $http.post('/lr', lrRequest);			
			//return playRoutes.controllers.Matching.getLR().post(lrRequest);
		};
		
		this.getComparedMixtureGene = function(profiles,matchId,isCollapsing) {
			// Asumiendo que profiles es una lista de códigos globales
			return $http.get('/mixture/compare?globalCodes=profiles&matchId=matchId&isCollapsing=isCollapsing');
			//return playRoutes.controllers.Matching.getComparedMixtureGene(profiles,matchId,isCollapsing).get();
		};

        this.deleteByLeftProfile = function(globalCode,courtCaseId) {
			return $http.delete('/collapsing/partial', {
				params: {
					globalCode: globalCode,
					courtCaseId: courtCaseId
				}
			});
            //return playRoutes.controllers.Matching.deleteByLeftProfile(globalCode,courtCaseId).delete();
        };

        this.confirmSelectedCollapsing = function(globalCodeParent,globalCodeChildren,courtCaseId) {
			var request = {};
			request.globalCodeParent = globalCodeParent;
			request.globalCodeChildren = globalCodeChildren;
			request.courtCaseId = parseInt(courtCaseId);
			console.log('CONFIRM SELECTED COLLAPSING');
			return $http.post('/collapsing/groups', request);
			//return playRoutes.controllers.Pedigrees.confirmSelectedCollapsing().post(request);
        };
        //this.deleteByLeftAndRightProfile = function(globalCode,courtCaseId) {
        //return playRoutes.controllers.Matching.deleteByLeftAndRightProfile(globalCode,courtCaseId).delete();
        //};
		
		this.getStrigencyEnum = function() {
			return { HighStringency : {key:'HighStringency', text:'Alta', css:'icon-highstringency'}, 
				ModerateStringency : {key:'ModerateStringency', text:'Media', css:'icon-moderatestringency'}, 
				LowStringency : {key:'LowStringency', text:'Baja', css:'icon-lowstringency'},
				Mismatch : {key:'Mismatch', text:'Sin match', css:'icon-mismatch'},
				NoMatch : {key:'NoMatch', text:'N/A', css:'icon-nomatch'}}; // enums
		};

		this.getMatchStatusEnum = function() {
			return { hit : 'Confirmado', discarded: 'Descartado', pending: 'Pendiente', deleted: 'Borrado'};
		};

		this.getSubCatName = function(groups, catId){
			for ( var grpId in groups) {
				var categories = groups[grpId].subcategories;
				for (var i = 0; i < categories.length; i++) {
					var category = categories[i];
					if(category.id === catId) {
						return groups[grpId].name  + " / " + category.name;
					}
				}
			}
			return catId;
		};

		this.descarteMasivoByGlobalCode = function (globalCode) {
			console.log('MASSIVE DISCARD BY GLOBAL');
			return $http.post('/masiveDiscardByGlobalCode', { firingCode: globalCode });
			//return playRoutes.controllers.Matching.masiveDiscardByGlobalCode(globalCode).post();
		};

		this.descarteMasivoByList = function (globalCode, matches) {
			console.log('MASSIVE DISCARD BY MATCHES');
			return $http.post('/masiveDiscardByMatchesList', { firingCode: globalCode, matches: matches });
			//return playRoutes.controllers.Matching.masiveDiscardByMatchesList(globalCode, matches).post();
		};
	}

	return MatchesService;
});

function fillReducedStringencies(results){
	'use strict';
	
	results.forEach (function(match) {
		var data = match.matchingAlleles;
		//var initialStringencies = Object.keys(data).reduce(function(prev, key) {prev[data[key]] = 0; return prev;}, {});
		var initialStringencies = {'HighStringency': 0, 'ModerateStringency': 0, 'LowStringency': 0, 'Mismatch': 0};
		match.reducedStringencies = Object.keys(data).reduce(function(prev, key) {
				if( data[key] !== 'Mismatch' ){prev[match.stringency] += 1;}
				return prev;},initialStringencies);
		
		Object.keys(match.reducedStringencies).forEach(function (s){
			if (match.reducedStringencies[s] === 0){
				delete match.reducedStringencies[s];
			}
		});
		
	});
}