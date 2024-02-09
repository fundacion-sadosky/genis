define(
	['angular','lodash','jquery'],
	function(angular,_,$) {
		'use strict';
		function PedigreeMatchesCourtCaseController(
			$scope,
			$rootScope,
			pedigreeMatchesService,
			pedigreeService,
			profileService,
			matchesService
		) {
			var uniqueElementsReducer = function(acc, x){
				if (acc.indexOf(x)<0){acc.push(x);}
				return acc;
			};
			$scope.profileChunks = function(profileIds) {
				var chunkSize = 5;
				return profileIds
					.reduce(
						function(acc, x) {
							var lastIndex = acc.length - 1;
							if (lastIndex < 0 || acc[lastIndex].length >=chunkSize) {
								acc.push([]);
								lastIndex = lastIndex + 1;
							}
							acc[lastIndex].push(x);
							return acc;
						},
						[]
					);
			};
			$scope.getCaseName = function() {
				if ($scope.sumMatches === undefined) {return "";}
				var caseName = $scope
					.sumMatches
					.map(
						function(x){
							return x.matchCard && x.matchCard.courtCaseName || "";
						}
					)
					[0];
				return caseName;
			};
			$scope.matchPStatus = matchesService.getMatchStatusEnum();
			$scope.initCourtCase = function(courtCaseId) {
				$scope.courtCaseId = courtCaseId;
			};
			$scope.findMatches = function(filters) {
				var searchObject = {
					"idCourtCase": $scope.courtCaseId,
					"page": 0,
					"pageSize": 100,
					"group": "pedigree",
					"pageSizeMatch": 100,
					"pageMatch": 0,
					"sortField": "date",
					"ascending": false
				};
				var countTotalItems = function(response) {
					$scope.totalItems = response.headers('X-MATCHES-LENGTH');
					return $scope.totalItems;
				};
				var findPedigrees = function(totalItems) {
					if (totalItems === '0') { return; }
					return pedigreeMatchesService
						.findMatchesPedigree(searchObject);
				};
				var assignSumMatches = function(response) {
					$scope.sumMatches = response.data;
					return response.data;
				};
				var getPedigrees = function(sumMatches){
					$scope.pedigreesIds = sumMatches
						.map(function(x){return x.id;});
					var promises = sumMatches
						.map(
							function(x) {
								var id = x && x.matchCard && x.matchCard.id || undefined;
								return pedigreeService
									.getPedigree(id)
									.then(function (r) {return r.data;});
							}
						);
					return Promise.all(promises);
				};
				var assignPedigrees = function(response) {
					$scope.pedigrees = response;
					return response;
				};
				var getGenogramsMap = function(response) {
					$scope.genoMap = Object.fromEntries(
						response
							.map(function(x) {return x.pedigreeGenogram;})
							.map(
								function(x) { return [x._id, x];}
							)
					);
					return response;
				};
				var collectProfiles = function(pedigrees) {
					$scope.pedigreeProfileIds = pedigrees
						.flatMap(
							function(x) {
								return Object
									.entries(x.pedigreeGenogram.genogram)
									.map(
										function (y){
											return y[1].globalCode;
										}
									);
							}
						)
						.reduce(uniqueElementsReducer, [])
						.filter(function(x) {return x!== undefined;});
					$scope.matchingProfileIds = $scope
						.sumMatches
						.flatMap(
							function(x) {
								var matchCardPeds = x && x.matchCardPed || [];
								return matchCardPeds.map(function(y){return y.internalCode;});
							}
						)
						.reduce(uniqueElementsReducer, []);
					var profiles = $scope.pedigreeProfileIds.concat($scope.matchingProfileIds);
					return Promise.all(
						profiles.map(function(x) {return profileService.getProfile(x);})
					);
				};
				var assignProfiles = function(response) {
					$scope.profiles = response;
				};
				var collectMarkers = function() {
					var selectNoMitoMarkers = function(m){return m[0] !== "4";};
					var markersAsObject = function (m) {
						return Object.entries(m[1]);
					};
					var markers = $scope
						.profiles
						.map(
							function(x){
								var markerMap =
									Object
										.entries(x.data.genotypification)
										.filter(selectNoMitoMarkers)
										.flatMap(markersAsObject);
								markerMap = Object.fromEntries(markerMap);
								return [x.data.globalCode, markerMap];
							}
						);
					$scope.markerMap = Object.fromEntries(markers);
					var getMarkerNames = function(x){
						return Object
							.entries(x[1])
							.map(function (x){return x[0];});
					};
					$scope.markers = markers
						.flatMap(getMarkerNames)
						.reduce(uniqueElementsReducer, [])
						.sort();
				};
				var collectMitoMarkers = function() {
					var pedigreeMitoData = $scope
						.pedigrees
						.map(function(x) {return x.pedigreeGenogram;})
						.map(
							function(geno){
								return [
									geno._id,
									Object.fromEntries(
										[
											["isMito", geno.executeScreeningMitochondrial],
											["nMismatches", geno.numberOfMismatches]
										]
									)
								];
							}
						);
					$scope.pedigreeMitoData = Object.fromEntries(pedigreeMitoData);
					var selectMitoMarkers = function(m){return m[0] === "4";};
					var markersAsObject = function (m) {
						return Object.entries(m[1]);
					};
					var mitoMarkers = $scope
						.profiles
						.map(
							function(x){
								var markerMap =
									Object
										.entries(x.data.genotypification)
										.filter(selectMitoMarkers)
										.flatMap(markersAsObject);
								markerMap = Object.fromEntries(markerMap);
								return [x.data.globalCode, markerMap];
							}
						);
					var selectMitoRanges = function(m) {
						return m[0].endsWith("RANGE");
					};
					var selectMitoAlleles = function(m) {
						return !m[0].endsWith("RANGE");
					};
					$scope.mitoRanges = Object
						.fromEntries(
							mitoMarkers
								.map(
									function(x) {
										return [
											x[0],
											Object
												.entries(x[1])
												.filter(selectMitoRanges)
												.map(function(x){return x[1];})
										];
									}
								)
						);
					$scope.mitoAlleles = Object
						.fromEntries(
							mitoMarkers
								.map(
									function(x) {
										return [
											x[0],
											Object
												.entries(x[1])
												.filter(selectMitoAlleles)
												.flatMap(function(x){return x[1];})
										];
									}
								)
						);
				};
				var replaceUnknownInGenomgram = function(genogram, replaceId) {
					var newGeno = _.cloneDeep(genogram);
					return newGeno
						.map(function(x) {
							if (x.unknown) {
								x.globalCode = replaceId;
							}
							return x;
						}
					);
				};
				var prepareProfilesForRender = function() {
					$scope.profilesForRender = $scope
						.sumMatches
						.flatMap(
							function(ped) {
								return ped.matchCardPed.map(
									function(m) {
										var correctedGeno = replaceUnknownInGenomgram(
											$scope.genoMap[ped.matchCard.id].genogram,
											m.internalCode
										);
										return [
											m.internalCode,
											m.sampleCode,
											ped.matchCard.title,
											$scope.genoMap[ped.matchCard.id],
											correctedGeno,
											$scope.profileChunks(
												correctedGeno.map(function(x){return [x.globalCode, x];})
											)
										];
									}
								);
							}
						);
				};
				var sendSummaryReadyEvent = function() {
					$rootScope.$broadcast('summaryReady');
				};
				pedigreeMatchesService
					.countMatches(searchObject)
					.then(countTotalItems)
					.then(findPedigrees)
					.then(assignSumMatches)
					.then(getPedigrees)
					.then(assignPedigrees)
					.then(getGenogramsMap)
					.then(collectProfiles)
					.then(assignProfiles)
					.then(collectMarkers)
					.then(collectMitoMarkers)
					.then(prepareProfilesForRender)
					.then(sendSummaryReadyEvent);
			};
			$scope.findMatches({});
		}
		return PedigreeMatchesCourtCaseController;
	}
);
