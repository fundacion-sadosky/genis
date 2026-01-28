define([],function() {
    'use strict';

    function ProfileExporterService(playRoutes,userService) {

        this.exporterProfiles = function(search) {
            var user = userService.getUser();
            search.user = user.name;
            search.isSuperUser = user.superuser;
            return playRoutes.controllers.Profiles.exporterProfiles().post(search);
        };
        this.getCategories = function() {
            return playRoutes.controllers.Categories.categoryTree().get();
        };
        this.getLaboratories = function(){
            return playRoutes.controllers.Laboratories.list().get();
        };

    }

    return ProfileExporterService;

});