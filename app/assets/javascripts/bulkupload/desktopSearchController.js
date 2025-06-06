define(
    [],
    function() {
        'use strict';
        function DesktopSearchController ($scope, matchesService, notificationsService, profileId) {

            var inicializar = function () {
                $scope.matches = [];
                $scope.stall = false;
            };
            inicializar();

            notificationsService.onMatchStatusNotification(function(msg){
                var status = msg.status;
                if (status === "started"){
                    $scope.stall = false;
                    $scope.working = true;
                    $scope.fail = false;
                    console.log("Match started");
                }else if (status === "ended"){
                    $scope.stall = true;
                    $scope.working = false;
                    $scope.fail = false;
                    console.log("Match ended");
                }else if (status === "fail"){
                    $scope.stall = false;
                    $scope.working = false;
                    $scope.fail = true;
                    console.log("Match failed");
                }else if (status === "pedigreeStarted"){
                    $scope.pedigreeStall = false;
                    $scope.pedigreeWorking = true;
                    console.log("Pedigree Match started");
                } else if (status === "pedigreeEnded"){
                    $scope.pedigreeStall = true;
                    $scope.pedigreeWorking = false;
                    console.log("Pedigree Match ended");
                }

                $scope.$apply();

            });

            notificationsService.onNotification(function(msg){
                    if (msg.kind === 'matching'){
                        var url = msg.url;
                        var regex = url.match(/\/comparison\/[^\/]+\/matchedProfileId\/([^\/]+)\/matchingId\//);
                        console.log("url: ", url, "regex: ", regex[1]);
                        if (regex && regex[1]){
                            var matchedProfileId = regex[1];
                            $scope.matches.push(matchedProfileId);
                            console.log("New match found:", matchedProfileId);
                        }
                    }
                });


            $scope.getMatches = function () {
                matchesService.searchMatchesProfile(profileId).then(function (response) {
                    console.log("Matches encontrados", response.data);
                    $scope.matches = response.data;
                });
            };

            $scope.$watch('stall', function (newVal, oldVal) {
                console.log("stall changed from", oldVal, "to", newVal);
                if (newVal === true && oldVal === false) {
                    //$scope.getMatches();
                }
            });


            $scope.cancel = function () {
                $scope.$dismiss('cancel');
            };

        }
        return DesktopSearchController;
    });