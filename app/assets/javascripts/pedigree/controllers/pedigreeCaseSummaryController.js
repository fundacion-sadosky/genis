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
				// var searchObject = {
				//	caseType: $scope.caseType,
				//	idCourtCase: $scope.idCourtCase,
				//	group: $scope.grupoId
				// };
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
				pedigreeMatchesService
					.countMatches(searchObject)
					.then(
						function(response) {
							$scope.totalItems = response.headers('X-MATCHES-LENGTH');
							return $scope.totalItems;
						}
					)
					.then(
						function(totalItems) {
							if ($scope.totalItems === '0') {
								return;
							}
							pedigreeMatchesService
								.findMatchesPedigree(searchObject)
								.then(
									function(response) {
										$scope.sumMatches = response.data;
									}
								)
								.then(
									function(){
										pedigreeService
											.getPedigree(17)
											.then(
													function(response) {
														$scope.pedigrees = response.data;
														console.log($scope.pedigrees);
													}
												);
									}
								);
						}
					);
			};
			$scope.findMatches({});
		}
		return PedigreeMatchesCourtCaseController;
	}
);
