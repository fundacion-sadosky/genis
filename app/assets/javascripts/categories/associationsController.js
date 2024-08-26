define(['angular'], function(ng) {
'use strict';

function AssociationsController($scope) {

	$scope.newAssociation = {};

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

	$scope.isNotAssociated = function(cat) {
        var category = $scope.categories[$scope.currCatId];
        return category && category.associations && !category.associations.hasOwnProperty(cat);
	};

	$scope.removeAssociation = function(association) {
		var category = $scope.categories[$scope.currCatId];
		var index = category.associations.indexOf(association);
        category.associations.splice(index, 1);
	};

	$scope.addAssociation = function() {
        if($scope.newAssociation.categoryRelated !== undefined) {
            var category = $scope.categories[$scope.currCatId];
            category.associations.push(ng.extend({type: $scope.activeAnalysis}, $scope.newAssociation));
            $scope.newAssociation = {};
        }
        if ($scope.associationForm) {
            $scope.associationForm.$setPristine();
        }
	};

    $scope.$on('form-reset', function(){
        $scope.newAssociation = {};
    });
    
}

return AssociationsController;

});