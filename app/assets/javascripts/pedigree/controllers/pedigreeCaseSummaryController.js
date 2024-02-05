define(
	['angular','lodash','jquery'],
	function(angular,_,$) {
		'use strict';
		function PedigreeMatchesCourtCaseController(
			$scope,
			pedigreeMatchesService,
			pedigreeService
		) {
			$scope.findMatches = function(filters) {
				var searchObject = {
					"caseType": "MPI",
					"idCourtCase": 7,
					"page": 0,
					"pageSize": 30,
					"group": "pedigree",
					"pageSizeMatch": 30,
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
					console.log("sumMatches", $scope.sumMatches);
					return response.data;
				};
				var getPedigrees = function(sumMatches){
					$scope.pedigreesIds = sumMatches
						.map(function(x){return x.id;});
					var result = sumMatches.map(
						function(x) {
							return pedigreeService
								.getPedigree(x.id)
								.then(
									function (r) {return r.data;}
								);
						}
					);
					return result;
				};
				var assignPedigrees = function(response) {
					$scope.pedigrees = response.data;
					console.log($scope.pedigrees);
				};
				pedigreeMatchesService
					.countMatches(searchObject)
					.then(countTotalItems)
					.then(findPedigrees)
					.then(assignSumMatches)
					.then(getPedigrees)
					.then(assignPedigrees);
			};
			$scope.findMatches({});
			$scope.findMatches({});
		}
		return PedigreeMatchesCourtCaseController;
	}
);
