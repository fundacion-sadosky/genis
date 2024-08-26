define([], function() {
'use strict';

function SearchService(playRoutes, userService){

	this.search = function(search) {
		var user = userService.getUser();
		search.userId = user.name;
		search.isSuperUser = user.superuser;
		return playRoutes.controllers.SearchProfileDatas.search().post(search);
	};

    this.searchTotal = function(search) {
		var user = userService.getUser();
		search.userId = user.name;
		search.isSuperUser = user.superuser;
        return playRoutes.controllers.SearchProfileDatas.searchTotal().post(search);
    };
	
	this.searchMatches = function(filters) {
		return playRoutes.controllers.SearchMatches.search().post(filters);
	};
	
	this.searchProfilesAssociable = function(input,category){
		return playRoutes.controllers.SearchProfileDatas.searchProfilesAssociable(input, category).get();
	};
    this.getMotives = function()  {
        var motiveTypeReject =  2;
        return playRoutes.controllers.MotiveController.getMotives(motiveTypeReject,false).get();
    };

	this.getCategories = function()  {
		return playRoutes.controllers.Categories.categoryTree().get();
	};

	this.getCategoriesWithProfiles = function(){
		return playRoutes.controllers.Categories.listWithProfiles().get();
    };

}

	return SearchService;
});