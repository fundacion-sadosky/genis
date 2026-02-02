#!/bin/bash
# GENis Configuration Switch Script
# Usage: ./switch.sh [legacy|modern]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  GENis Configuration Switch${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo ""
}

print_usage() {
    echo -e "${YELLOW}Usage:${NC} ./switch.sh [legacy|modern]"
    echo ""
    echo "  legacy  - Play 2.3.10, Scala 2.11.11, sbt 0.13.15, Java 8"
    echo "            Uses: app/"
    echo ""
    echo "  modern  - Play 3.0.6,  Scala 3.3.1,   sbt 1.10.5,  Java 17"
    echo "            Uses: modules/core/"
    echo ""
}

clean_caches() {
    echo -e "${YELLOW}[2/4] Cleaning ALL sbt caches and generated folders...${NC}"
    
    # Main project caches
    rm -rf target/ 2>/dev/null || true
    
    # Project folder caches (THIS IS CRITICAL - sbt freezes if these are stale)
    rm -rf project/target/ 2>/dev/null || true
    rm -rf project/project/ 2>/dev/null || true
    
    # IDE/BSP caches
    rm -rf .bsp/ 2>/dev/null || true
    rm -rf .metals/ 2>/dev/null || true
    rm -rf .bloop/ 2>/dev/null || true
    rm -rf .idea/libraries/ 2>/dev/null || true
    rm -rf .idea/modules/ 2>/dev/null || true
    rm -rf .idea/*.iml 2>/dev/null || true
    rm -f .idea/sbt.xml 2>/dev/null || true
    
    # Module caches
    rm -rf modules/core/target/ 2>/dev/null || true
    rm -rf modules/shared/target/ 2>/dev/null || true
    rm -rf app/target/ 2>/dev/null || true
    
    # Ivy resolution caches for this project (forces re-resolve)
    rm -rf ~/.sbt/1.0/plugins/target/ 2>/dev/null || true
    rm -rf ~/.sbt/0.13/plugins/target/ 2>/dev/null || true
    
    echo -e "  ${GREEN}✓${NC} All caches cleaned"
}

switch_to_legacy() {
    echo -e "${YELLOW}[1/4] Switching to LEGACY configuration...${NC}"
    
    if [ ! -d "config-legacy" ]; then
        echo -e "${RED}Error: config-legacy/ folder not found!${NC}"
        exit 1
    fi
    
    # Copy config files
    cp config-legacy/build.sbt build.sbt
    cp config-legacy/project/build.properties project/build.properties
    cp config-legacy/project/plugins.sbt project/plugins.sbt
    
    # Copy Build.scala (required for sbt 0.13.15)
    if [ -f "config-legacy/project/Build.scala" ]; then
        cp config-legacy/project/Build.scala project/Build.scala
        echo -e "  ${GREEN}✓${NC} Build.scala copied"
    fi
    
    echo -e "  ${GREEN}✓${NC} Configuration files copied"
    
    clean_caches
    
    echo -e "${YELLOW}[3/4] Configuration summary:${NC}"
    echo -e "  • Play:  ${GREEN}2.3.10${NC}"
    echo -e "  • Scala: ${GREEN}2.11.11${NC}"
    echo -e "  • sbt:   ${GREEN}0.13.15${NC}"
    echo -e "  • Java:  ${GREEN}8${NC}"
    echo -e "  • Code:  ${GREEN}app/${NC}"
    
    echo ""
    echo -e "${YELLOW}[4/4] IMPORTANT - Set Java version:${NC}"
    echo -e "  ${RED}Run this command:${NC}"
    echo ""
    echo -e "    ${GREEN}sdk use java 8.0.472-amzn${NC}"
    echo ""
    echo -e "  (or your installed Java 8 version from 'sdk list java')"
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Ready! Run 'sbt run' to start legacy on port 9000${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
}

switch_to_modern() {
    echo -e "${YELLOW}[1/4] Switching to MODERN configuration...${NC}"
    
    if [ ! -d "config-modern" ]; then
        echo -e "${RED}Error: config-modern/ folder not found!${NC}"
        exit 1
    fi
    
    # Copy config files
    cp config-modern/build.sbt build.sbt
    cp config-modern/project/build.properties project/build.properties
    cp config-modern/project/plugins.sbt project/plugins.sbt
    
    # Remove Build.scala (not used in sbt 1.x / modern)
    if [ -f "project/Build.scala" ]; then
        rm -f project/Build.scala
        echo -e "  ${GREEN}✓${NC} Build.scala removed (modern uses only .sbt)"
    fi
    
    echo -e "  ${GREEN}✓${NC} Configuration files copied"
    
    clean_caches
    
    echo -e "${YELLOW}[3/4] Configuration summary:${NC}"
    echo -e "  • Play:  ${GREEN}3.0.6${NC}"
    echo -e "  • Scala: ${GREEN}3.3.1${NC}"
    echo -e "  • sbt:   ${GREEN}1.10.5${NC}"
    echo -e "  • Java:  ${GREEN}17${NC}"
    echo -e "  • Code:  ${GREEN}modules/core/${NC} (NOT app/)"
    
    echo ""
    echo -e "${YELLOW}[4/4] IMPORTANT - Set Java version:${NC}"
    echo -e "  ${RED}Run this command:${NC}"
    echo ""
    echo -e "    ${GREEN}sdk use java 17.0.17-amzn${NC}"
    echo ""
    echo -e "  (or your installed Java 17 version from 'sdk list java')"
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Ready! Run 'sbt run' to start modern on port 9001${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
}

# Main
print_header

case "${1:-}" in
    legacy)
        switch_to_legacy
        ;;
    modern)
        switch_to_modern
        ;;
    *)
        print_usage
        exit 1
        ;;
esac
