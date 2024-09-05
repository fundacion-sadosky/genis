define([ 'angular' ], function(angular) {
	'use strict';

	function SearchService(playRoutes, userService, $http){

		this.search = function(search) {
			var user = userService.getUser();
			search.userId = user.name;
			search.isSuperUser = user.superuser;
			console.log("SEARCH");
			return  $http.post('/search/profileData/search', search);
			//return playRoutes.controllers.SearchProfileDatas.search().post(search);
		};

		this.searchTotal = function(search) {
			var user = userService.getUser();
			search.userId = user.name;
			search.isSuperUser = user.superuser;
			console.log('SEARCH TOTAL');
            return  $http.post('/search/profileData/searchTotal', search);
			//return playRoutes.controllers.SearchProfileDatas.searchTotal().post(search);
		};

		this.searchMatches = function(filters) {
			return playRoutes.controllers.SearchMatches.search().post(filters);
		};

		this.searchProfilesAssociable = function(input,category){
			return $http.get('/search/profileData/' + encodeURIComponent(category), { params: { input: input } });
			//return playRoutes.controllers.SearchProfileDatas.searchProfilesAssociable(input, category).get();
		};

		this.getMotives = function()  {
			var motiveTypeReject =  2;
			return $http.get('/motive', { params: { id: motiveTypeReject, abm: false } });
			//return playRoutes.controllers.MotiveController.getMotives(motiveTypeReject,false).get();
		};

		this.getCategories = function()  {
			return $http.get('/categoryTree');
			//return playRoutes.controllers.Categories.categoryTree().get();
		};

		this.getCategoriesWithProfiles = function(){
			return $http.get('/categoriesWithProfiles');
			//return playRoutes.controllers.Categories.listWithProfiles().get();
		};

	}

	return SearchService;
});