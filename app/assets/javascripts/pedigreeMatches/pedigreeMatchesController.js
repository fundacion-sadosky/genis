define(['angular', 'lodash', 'jquery'], function (angular, _, $) {
  'use strict';

  function PedigreeMatchesController(
    $scope,
    pedigreeMatchesService,
    matchesService,
    alertService,
    profiledataService,
    pedigreeService,
    profileService,
    $anchorScroll,
    $timeout
  ) {
    $scope.stringency = matchesService.getStrigencyEnum();
    $scope.isProcessing = false;
    $scope.noResult = true;
    $scope.search = {};
    $scope.previousSearch = {};

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("nuevo");

    var uniqueElementsReducer = function (acc, x) {
      if (acc.indexOf(x) < 0) {
        acc.push(x);
      }
      return acc;
    };
    $scope.profileChunks = function (profileIds) {
      var chunkSize = 5;
      return profileIds
        .reduce(
          function (acc, x) {
            var lastIndex = acc.length - 1;
            if (lastIndex < 0 || acc[lastIndex].length >= chunkSize) {
              acc.push([]);
              lastIndex = lastIndex + 1;
            }
            acc[lastIndex].push(x);
            return acc;
          },
          []
        );
    };

    $scope.status = [
      {label: "todos", value: "", index: 0},
      {label: "$.i18n.t('generics.pending')", value: "pending", index: 1},
      {label: "$.i18n.t('generics.confirmedPlural')", value: "hit", index: 2},
      {label: "$.i18n.t('generics.discarded')", value: "discarded", index: 3}
    ];
    if (localStorage.length > 0 && localStorage.getItem("searchPedigreeMatches")) {
      var inicio = JSON.parse(localStorage.getItem("searchPedigreeMatches"));
      var index = $scope.status.filter(
        function (item) {
          return item.value === inicio.status;
        }
      );
      $scope.currentPage = inicio.page + 1;
      $scope.categoryCode = inicio.categoria;
      $scope.caseType = inicio.caseType;

      if (index[0] === undefined) {
        $scope.tab = $scope.status[0].value;
        $scope.indexFiltro = $scope.status[0].index;
        $scope.clase = "tab-estados-tab__" + $scope.status[0].label;
      } else {
        $scope.tab = index[0].value;
        $scope.indexFiltro = index[0].index;
        $scope.clase = "tab-estados-tab__" + index[0].label;
      }
    } else {
      $scope.currentPage = 1;
      $scope.tab = $scope.status[1].value;
      $scope.indexFiltro = $scope.status[1].index;
      $scope.clase = "tab-estados-tab__pendientes";
      $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
    }

    $scope.matchPStatus = matchesService.getMatchStatusEnum();

    $scope.datepickers = {
      hourFrom: false,
      hourUntil: false
    };

    $scope.dateOptions = {
      initDate: new Date()
    };

    $scope.currentPage = 1;
    $scope.pageSize = 30;

    $scope.groupId = 'pedigree';

    $scope.matchPStatus = matchesService.getMatchStatusEnum();

    $scope.datepickers = {
      hourFrom: false,
      hourUntil: false
    };

    $scope.dateOptions = {
      initDate: new Date()
    };

    $scope.currentPage = 1;
    $scope.pageSize = 30;

    $scope.groupId = 'pedigree';

    $scope.toggleDatePicker = function ($event, witch) {
      $event.preventDefault();
      $event.stopPropagation();
      $scope.datepickers[witch] = !$scope.datepickers[witch];
    };

    var createSearchObject = function (filters) {
      var searchObject = {};
      if (filters.profile && filters.profile.length > 0) {
        searchObject.profile = filters.profile;
      }
      if (filters.category && filters.category.length > 0) {
        searchObject.category = filters.category;
      }
      searchObject.caseType = "MPI";
      searchObject.hourFrom = filters.hourFrom;
      searchObject.hourUntil = filters.hourUntil;
      if (searchObject.hourUntil) {
        searchObject.hourUntil.setHours(23);
        searchObject.hourUntil.setMinutes(59);
        searchObject.hourUntil.setSeconds(59);
      }

      if ($scope.tab && $scope.tab.length > 0) {
        searchObject.status = $scope.tab;
      }
      searchObject.page = $scope.currentPage - 1;
      searchObject.pageSize = $scope.pageSize;
      searchObject.group = $scope.groupId;

      if (localStorage.length > 0 && localStorage.getItem("searchPedigreeMatches")) {
        searchObject = JSON.parse(localStorage.getItem("searchPedigreeMatches"));
        $scope.currentPage = searchObject.page + 1;
        $scope.pageSize = searchObject.pageSize;
        $scope.search.hourUntil = searchObject.hourUntil;
        $scope.search.hourFrom = searchObject.hourFrom;
        $scope.search.category = searchObject.category;
        $scope.tab = searchObject.status;
        $scope.search.profile = searchObject.profile;
        $scope.idHash = searchObject.globalFocus;
        $scope.groupId = searchObject.group;
        $scope.caseType = searchObject.caseType;
      }
      return searchObject;
    };


    $scope.findMatchesForSummary = function (filters) {
      var searchObject = createSearchObject(filters);
      searchObject.group = "profile";
      searchObject.pageSizeMatch = 100;
      searchObject.pageSize = 100;
      searchObject.pageMatch = 0;
      searchObject.sortField = "date";
      searchObject.ascending = false;

      var assignMatchesSumm = function (response) {
        $scope.matchesSumm = response.data;
      };

      var getPedigrees = function () {
        var courtCases = $scope
          .matchesSumm
          .flatMap(function (x) {
            return x.matchCardPed.map(function (x) {
              return x.courtCaseId;
            });
          })
          .reduce(uniqueElementsReducer, []);
        return Promise.all(
          courtCases
            .map(
              function (x) {
                return pedigreeService.getPedigreesByCourtCase(x);
              }
            )
        );
      };

      var buildCourtCaseMap = function (response) {
        $scope.courtCaseMap = response
          .flatMap(function (x) {
            return x.data;
          })
          .map(
            function (x) {
              return [x.idCourtCase, x];
            }
          )
          .reduce(
            function (acc, x) {
              if (!acc.hasOwnProperty(x[0])) {
                acc[x[0]] = [];
              }
              acc[x[0]].push(x[1]);
              return acc;
            },
            {}
          );
      };

      var collectProfiles = function () {
        $scope.pedigreeProfileIds = Object
          .entries($scope.courtCaseMap)
          .flatMap(function (x) {
            return x[1];
          })
          .flatMap(function (x) {
            return x.genogram;
          })
          .map(function (x) {
            return x.globalCode;
          })
          .filter(function (x) {
            return x !== undefined;
          })
          .reduce(uniqueElementsReducer, []);

        $scope.matchingProfileIds = $scope
          .matchesSumm
          .map(
            function (x) {
              return x.matchCard.id || "";
            }
          )
          .reduce(uniqueElementsReducer, []);
        var profiles = $scope.pedigreeProfileIds.concat($scope.matchingProfileIds);
        return Promise.all(
          profiles.map(function (x) {
            return profileService.getProfile(x);
          })
        );
      };

      var assignProfiles = function (response) {
        $scope.profiles = response;
      };

      var collectMarkers = function () {
        var selectNoMitoMarkers = function (m) {
          return m[0] !== "4";
        };
        var markersAsObject = function (m) {
          return Object.entries(m[1]);
        };
        var markers = $scope
          .profiles
          .map(
            function (x) {
              var markerMap =
                Object
                  .entries(x.data.genotypification)
                  .filter(selectNoMitoMarkers)
                  .flatMap(markersAsObject);
              markerMap = Object.fromEntries(markerMap);
              return [x.data.globalCode, markerMap];
            }
          );
        $scope.markerMap = Object.fromEntries(markers);
        var getMarkerNames = function (x) {
          return Object
            .entries(x[1])
            .map(function (x) {
              return x[0];
            });
        };
        $scope.markers = markers
          .flatMap(getMarkerNames).reduce(uniqueElementsReducer, [])
          .sort();
      };
      var collectMitoMarkers = function () {
        var pedigreeMitoData = Object
          .entries($scope.courtCaseMap)
          .flatMap(function (x) {
            return x[1];
          })
          .map(
            function (geno) {
              return [
                geno._id,
                Object.fromEntries(
                  [
                    ["isMito", geno.executeScreeningMitochondrial],
                    ["nMismatches", geno.numberOfMismatches]
                  ]
                )
              ];
            }
          );
        $scope.pedigreeMitoData = Object.fromEntries(pedigreeMitoData);
        var selectMitoMarkers = function (m) {
          return m[0] === "4";
        };
        var markersAsObject = function (m) {
          return Object.entries(m[1]);
        };
        var mitoMarkers = $scope
          .profiles
          .map(
            function (x) {
              var markerMap =
                Object
                  .entries(x.data.genotypification)
                  .filter(selectMitoMarkers)
                  .flatMap(markersAsObject);
              markerMap = Object.fromEntries(markerMap);
              return [x.data.globalCode, markerMap];
            }
          );
        var selectMitoRanges = function (m) {
          return m[0].endsWith("RANGE");
        };
        var selectMitoAlleles = function (m) {
          return !m[0].endsWith("RANGE");
        };
        $scope.mitoRanges = Object
          .fromEntries(
            mitoMarkers
              .map(
                function (x) {
                  return [
                    x[0],
                    Object
                      .entries(x[1])
                      .filter(selectMitoRanges)
                      .map(function (x) {
                        return x[1];
                      })
                  ];
                }
              )
          );
        $scope.mitoAlleles = Object
          .fromEntries(
            mitoMarkers
              .map(
                function (x) {
                  return [
                    x[0],
                    Object
                      .entries(x[1])
                      .filter(selectMitoAlleles)
                      .flatMap(function (x) {
                        return x[1];
                      })
                  ];
                }
              )
          );
      };
      var replaceUnknownInGenomgram = function (genogram, replaceId) {
        var newGeno = _.cloneDeep(genogram);
        return newGeno
          .map(function (x) {
              if (x.unknown) {
                x.globalCode = replaceId;
              }
              return x;
            }
          );
      };
      var prepareProfilesForRender = function () {
        $scope.profilesForRender = $scope.matchesSumm
          .flatMap(
            function (x) {
              return x.matchCardPed.map(
                function (y) {
                  return [x.matchCard, y];
                }
              );
            }
          )
          .flatMap(
            function (x) {
              return $scope.courtCaseMap[x[1].courtCaseId]
                .filter(function (y) {
                  return y._id === x[1].idPedigree;
                })
                .map(
                  function (y) {
                    return [
                      x[0],
                      x[1],
                      y,
                      $scope.profileChunks(
                        replaceUnknownInGenomgram(y.genogram, x[0].id)
                          .map(function (x) {
                            return [x.globalCode, x];
                          })
                      )
                    ];
                  }
                );
            }
          );
        return $scope.profilesForRender !== undefined;
      };
      return pedigreeMatchesService
        .findMatchesPedigree(searchObject)
        .then(assignMatchesSumm)
        .then(getPedigrees)
        .then(buildCourtCaseMap)
        .then(collectProfiles)
        .then(assignProfiles)
        .then(collectMarkers)
        .then(collectMitoMarkers)
        .then(prepareProfilesForRender);
    };

    $scope.findMatches = function (filters) {
      $scope.isProcessing = true;
      $scope.noResult = false;
      var searchObject = createSearchObject(filters);
      $scope.busqueda = searchObject;
      pedigreeMatchesService.countMatches(searchObject).then(function (response) {
        $scope.totalItems = response.headers('X-MATCHES-LENGTH');

        if ($scope.totalItems !== '0') {
          pedigreeMatchesService.findMatches(searchObject).then(function (response) {
            $scope.matches = response.data;
            localStorage.removeItem("searchPedigreeMatches");
            if ($scope.idHash !== undefined) {
              $timeout(function () {
                $anchorScroll();

              });
            }
            $scope.isProcessing = false;
          }, function () {
            $scope.isProcessing = false;
          });
        } else {
          $scope.noResult = true;
          $scope.isProcessing = false;
        }

      }, function () {
        $scope.isProcessing = false;
      });
    };

    $scope.clearSearch = function () {
      $scope.search = {};
      localStorage.removeItem("searchPedigreeMatches");
      $scope.categoryCode = undefined;
      $scope.previousSearch = {};
      $scope.idHash = undefined;
      $scope.findMatches($scope.previousSearch);
    };

    $scope.searchMatches = function () {
      $scope.previousSearch = angular.copy($scope.search);
      $scope.findMatches($scope.previousSearch);
    };

    profiledataService.getCategories().then(function (response) {
      $scope.cate = response.data;
      $scope.categorias = {};
      $scope.grupo = {};
      _.forEach($scope.cate, function (value, key) {
        if (key === "AM" || key === "PM") {
          var n = key;
          $scope.categorias[n] = value;
        }
      });
    });
    $scope.findMatches({});

    $scope.searchPrevious = function () {
      $scope.findMatches($scope.previousSearch);
    };

    $scope.getGroups = function (match) {
      var filtro = "&i=" + $scope.indexFiltro;
      return '#/pedigreeMatchesGroups/groups.html?g=' + match.groupBy + "&p=" + match.id + filtro;
    };

    $scope.maxDate = $scope.maxDate ? null : new Date();
    $scope.minDateCoin = null;

    $scope.checkMaxMin = function (fieldName, fechaMin) {
      var aux = $scope.search[fieldName];
      var min = $scope.search[fechaMin];
      var max = new Date();

      if (min === undefined || min === null) {
        min = $scope.search.hourFrom;
      }
      if (max - aux < 0) {
        alertService.info({message: $.i18n.t('alerts.date.before')});
        $scope.search[fieldName] = undefined;
      } else {
        if (min - aux > 0) {
          alertService.info({message: $.i18n.t('alerts.date.after')});
          $scope.search[fieldName] = undefined;
        }
      }

    };

    $scope.checkMax = function (fieldName) {
      var aux = $scope.search[fieldName];
      var today = new Date();

      if (today - aux < 0) {
        alertService.info({message: $.i18n.t('alerts.date.before')});
        $scope.search[fieldName] = undefined;
        $scope.minDateCoin = null;
      } else {
        if (fieldName === 'hourFrom') {
          if (aux !== undefined || aux !== null) {
            $scope.minDateCoin = aux;
          } else {
            $scope.minDateCoin = null;
          }
        }
      }

    };

    $scope.filtros = function (filtro) {
      var clase = "tab-estados-tab__" + filtro.label;
      if (clase !== $scope.clase) {
        $("div." + $scope.clase).removeClass($scope.clase + "__selected");
        $scope.clase = clase;
      }
      $scope.tab = filtro.value;
      $scope.indexFiltro = filtro.index;

      $("div." + $scope.clase).toggleClass($scope.clase + "__selected");
      $scope.searchMatches();
    };

    $scope.descarteMasivo = function (confirm, match) {
      if (!confirm) {
        return;
      }
      var matchObject = JSON.parse(match);
      pedigreeMatchesService.descarteMasivoByGroup(matchObject.id, matchObject.groupBy).then(function () {
        alertService.success({message: $.i18n.t('alerts.group.discarded')});
        $scope.searchMatches();
      }, function (response) {
        if (response.data.message === "Sin matches") {
          alertService.error({message: $.i18n.t('alerts.matches.noMatches')});
        } else {
          alertService.error({message: $.i18n.t('alerts.matches.noMatchesError')});
          $scope.searchMatches();
        }
      });
    };

    $scope.filtroLocal = function (id) {
      $scope.busqueda.globalFocus = id;
      localStorage.setItem("searchPedigreeMatches", JSON.stringify($scope.busqueda));
    };

    $scope.printSummaryReport = function () {
      $scope
        .findMatchesForSummary($scope.previousSearch || {})
        .then(
          function (success) {
            if (!success) {
              return;
            }
            var head = '<head><title>Resumen de coincidencias</title>';
            $("link").each(function () {
              head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
            });
            head += "</head>";
            $scope.$apply();
            var report = window.open('', '_blank');
            report.document.write(
              '<html>' + head +
              '<body>' +
              $('#report').html() +
              '</body></html>'
            );
            report.document.close();
            $(report).on('load', function () {
              report.print();
              report.close();
            });
          }
        );
    };
  }

  return PedigreeMatchesController;
});
