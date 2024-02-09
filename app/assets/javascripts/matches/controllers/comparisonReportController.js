define(
  ['angular', 'lodash'],
  function(angular, _) {
    'use strict';
    function ComparisonReportController(
      $scope
    ) {
      $scope.parent = $scope.$parent;
    }
    return ComparisonReportController;
  }
);