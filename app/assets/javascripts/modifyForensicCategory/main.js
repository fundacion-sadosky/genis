define(
  [
    'angular',
    './controllers/modifyForensicCategoryController'
  ],
  function(
    angular,
    modifyForensicCategoryController
  ) {
    'use strict';
    angular
      .module('pdg.modifyForensicCategory', ['pdg.common'])
      .controller(
        'modifyForensicCategoryController',
        [
          '$scope',
          '$rootScope',
          '$routeParams',
          '$log',
          'alertService',
          'searchService',
          'categoriesService',
          'profileDataService',
          modifyForensicCategoryController
        ]
      )
      .config(
        [
          '$routeProvider',
          function($routeProvider) {
            $routeProvider
              .when(
                '/modify-forensic-category',
                {
                  templateUrl: '/assets/javascripts/modifyForensicCategory/views/modifyForensicCategory.html',
                  controller: 'modifyForensicCategoryController'
               }
             );
          }
        ]
      );
    return undefined;
  }
);
