define([],function() {
			'use strict';

			function ProfileDataCommonService($q, playRoutes, $log, $http) {

				this.getCategories = function() {
					var deferred = $q.defer();
					var p1 = playRoutes.controllers.Categories.categoryTreeManualLoading().get();
					var p2 = playRoutes.controllers.Categories.list().get();

					$q.all({k1 : p1,k2 : p2}).then(function(a) {

						var tree = a.k1.data;
						var list = a.k2.data;

						for(var leaf in tree){
							var leafObject = tree[leaf];
							for(var catIndex in leafObject.subcategories){
								for(var elemIndex in list){
									if(leafObject.subcategories[catIndex].id === list[elemIndex].id){
										tree[leaf].subcategories[catIndex].filiationDataRequired = list[elemIndex].filiationDataRequired;
									}
								}
							}
						}
						deferred.resolve(tree);
					});
					return deferred.promise;
				};

				this.getCrimeTypes = function() {
					$log.info('calling service: CrimeTypes.list()');
					return $http.get('/crimeTypes');
					//return playRoutes.controllers.CrimeTypes.list().get();
				};

				this.getBioMaterialTypes = function() {
					$log.info('calling service: BioMaterialTypes.lis()');
					return $http.get('/bioMaterialTypes');
					//return playRoutes.controllers.BioMaterialTypes.list().get();
				};

				this.getGeneticist = function(lab) {
					$log.info('calling all geneticist for ' + lab);
					return $http.get('/geneticist/' + lab);
					//return playRoutes.controllers.Geneticists.allGeneticist(lab).get();
				};

				this.getGeneticistUsers = function() {
					$log.info('calling all geneticist users');
					return $http.get('/geneticist-users');
					//return playRoutes.controllers.Geneticists.getGeneticistUsers().get();
				};

				this.getLaboratories = function() {
					$log.info('calling service: all labs');
					return $http.get('/laboratory');
					//return playRoutes.controllers.Laboratories.list().get();
				};

				this.getFilesId = function() {
					$log.info('calling for token for images');
					return $http.get('/getFilesId');
					//return playRoutes.controllers.Resources.getFilesId().get();
				};
			}

			return ProfileDataCommonService;
		});
