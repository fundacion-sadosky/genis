define([], function() {
	'use strict';

	function StatsDbController($scope, $location, populationBaseService) {

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

		$scope.getBase = function() {
			var url = $location.url();
			var name = decodeURI(url.split("/")[2]);
			$scope.dest = name;
			populationBaseService.getBaseByName(name).then(function(response){
				$scope.populationBase = response.data;
			});
		};
	
	}
	
	return StatsDbController;
});
		