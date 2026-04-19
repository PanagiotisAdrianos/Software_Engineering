#!/bin/bash

# ============================================================
#  🧙 GIT WIZARD - Ένα script, όλα μέσα
# ============================================================

# ── ΛΟΓΑΡΙΑΣΜΟΙ (επεξεργάσου εδώ) ──────────────────────────
declare -A NAMES=(
  [1]="PanagiotisAdrianos"
  [2]="giannis262"
  [3]="dimitriskoukos1"
  [4]="Ppg132004"
  [5]="Nightm4res"
)

declare -A EMAILS=(
  [1]="pangiotisadrianos@gmail.com"
  [2]="giannisant13@gmail.com"
  [3]="dikoukos@icloud.com"
  [4]="ppg132004@gmail.com"
  [5]="incr3dible363@gmail.com"
)

declare -A SSH_KEYS=(
  [1]="$HOME/.ssh/id_ed25519_PanagiotisAdrianos"
  [2]="$HOME/.ssh/id_ed25519_giannis262"
  [3]="$HOME/.ssh/id_ed25519_dimitriskoukos1"
  [4]="$HOME/.ssh/id_ed25519_Ppg132004"
  [5]="$HOME/.ssh/id_ed25519_Nightm4res"
)

TOTAL=5  # Πόσοι λογαριασμοί υπάρχουν
# ────────────────────────────────────────────────────────────

# Χρώματα
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

divider() { echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"; }

# ── ΒΗΜΑ 1: Επιλογή λογαριασμού ─────────────────────────────
clear
divider
echo -e "  ${BOLD}🧙 GIT WIZARD${NC}"
divider
echo ""
echo -e "  ${CYAN}ΒΗΜΑ 1/3 — Διάλεξε λογαριασμό:${NC}"
echo ""

for i in $(seq 1 $TOTAL); do
  echo -e "  ${YELLOW}[$i]${NC} ${NAMES[$i]}  ${BLUE}<${EMAILS[$i]}>${NC}"
done

echo ""
read -p "  👉 Επιλογή (1-$TOTAL): " acc_choice

if [[ -z "${NAMES[$acc_choice]}" ]]; then
  echo -e "\n  ${RED}❌ Μη έγκυρη επιλογή. Έξοδος.${NC}\n"
  exit 1
fi

SELECTED_NAME="${NAMES[$acc_choice]}"
SELECTED_EMAIL="${EMAILS[$acc_choice]}"
SELECTED_KEY="${SSH_KEYS[$acc_choice]}"

# Εφαρμογή config
git config user.name  "$SELECTED_NAME"
git config user.email "$SELECTED_EMAIL"
git config core.sshCommand "ssh -i $SELECTED_KEY -o IdentitiesOnly=yes"

echo ""
echo -e "  ${GREEN}✅ Ενεργός λογαριασμός:${NC}"
echo -e "     👤 ${BOLD}$SELECTED_NAME${NC}"
echo -e "     📧 $SELECTED_EMAIL"
echo -e "     🔑 $SELECTED_KEY"

# ── ΒΗΜΑ 2: Τι θες να κάνεις; ────────────────────────────────
echo ""
divider
echo -e "  ${CYAN}ΒΗΜΑ 2/3 — Τι θες να κάνεις;${NC}"
echo ""
echo -e "  ${YELLOW}[1]${NC} Commit + Push"
echo -e "  ${YELLOW}[2]${NC} Μόνο Commit (χωρίς push)"
echo -e "  ${YELLOW}[3]${NC} Μόνο Push (υπάρχον commit)"
echo -e "  ${YELLOW}[4]${NC} Pull (ανανέωση από remote)"
echo -e "  ${YELLOW}[5]${NC} Δες status & diff πριν αποφασίσεις"
echo ""
read -p "  👉 Επιλογή (1-5): " action_choice

case $action_choice in

  5)
    # Status & Diff preview
    echo ""
    divider
    echo -e "  ${CYAN}📊 STATUS:${NC}"
    git status -s
    echo ""
    echo -e "  ${CYAN}📝 DIFF (τελευταίες αλλαγές):${NC}"
    git diff --stat
    echo ""
    read -p "  Συνέχεια; Τι θες τώρα; (1=Commit+Push / 2=Commit / 3=Push / q=Έξοδος): " action_choice
    [[ "$action_choice" == "q" ]] && echo "" && exit 0
    ;;

  4)
    # Pull
    echo ""
    divider
    echo -e "  ${CYAN}ΒΗΜΑ 3/3 — Pull${NC}"
    echo ""
    read -p "  Branch (Enter = τρέχον): " branch
    branch="${branch:-$(git branch --show-current)}"
    echo ""
    git pull origin "$branch"
    echo ""
    echo -e "  ${GREEN}✅ Pull ολοκληρώθηκε!${NC}"
    echo ""
    exit 0
    ;;
esac

# ── ΒΗΜΑ 3: Πληροφορίες commit/push ──────────────────────────
echo ""
divider
echo -e "  ${CYAN}ΒΗΜΑ 3/3 — Λεπτομέρειες${NC}"
echo ""

# Branch
CURRENT_BRANCH=$(git branch --show-current)
read -p "  🌿 Branch [${CURRENT_BRANCH}]: " branch
branch="${branch:-$CURRENT_BRANCH}"

# Commit message (μόνο αν χρειάζεται)
if [[ "$action_choice" == "1" || "$action_choice" == "2" ]]; then
  echo ""
  echo -e "  ${CYAN}Επιλογή τύπου commit:${NC}"
  echo -e "  ${YELLOW}[1]${NC} feat     — Νέο feature"
  echo -e "  ${YELLOW}[2]${NC} fix      — Bug fix"
  echo -e "  ${YELLOW}[3]${NC} chore    — Συντήρηση / config"
  echo -e "  ${YELLOW}[4]${NC} docs     — Αλλαγές σε documentation"
  echo -e "  ${YELLOW}[5]${NC} refactor — Αναδόμηση κώδικα"
  echo -e "  ${YELLOW}[6]${NC} Χωρίς prefix (custom μήνυμα)"
  echo ""
  read -p "  👉 Τύπος (1-6): " commit_type

  case $commit_type in
    1) prefix="feat: "     ;;
    2) prefix="fix: "      ;;
    3) prefix="chore: "    ;;
    4) prefix="docs: "     ;;
    5) prefix="refactor: " ;;
    *) prefix=""           ;;
  esac

  echo ""
  read -p "  💬 Commit message: ${prefix}" commit_msg
  FULL_MSG="${prefix}${commit_msg}"
fi

# ── ΕΚΤΕΛΕΣΗ ──────────────────────────────────────────────────
echo ""
divider
echo -e "  ${CYAN}⚙️  Εκτέλεση...${NC}"
echo ""

case $action_choice in

  1|2) # Commit + Push ή Μόνο Commit

    # Επιλογή αρχείων
    echo ""
    echo -e "  ${CYAN}Ποια αρχεία να συμπεριληφθούν;${NC}"
    echo ""
    echo -e "  ${YELLOW}[1]${NC} Όλα τα αρχεία ${BLUE}(git add -A)${NC}"
    echo -e "  ${YELLOW}[2]${NC} Συγκεκριμένα αρχεία ${BLUE}(τα γράφεις εσύ)${NC}"
    echo -e "  ${YELLOW}[3]${NC} Interactive — βλέπεις κάθε αλλαγή μία-μία ${BLUE}(git add -p)${NC}"
    echo ""
    read -p "  👉 Επιλογή (1-3): " add_choice

    case $add_choice in
      1)
        git add -A
        echo -e "  ${GREEN}✔ git add -A${NC}"
        ;;
      2)
        echo ""
        echo -e "  ${CYAN}Τρέχουσες αλλαγές:${NC}"
        git status -s
        echo ""
        read -p "  📁 Γράψε τα αρχεία (πχ: index.html src/app.js): " files
        git add $files
        echo -e "  ${GREEN}✔ git add $files${NC}"
        ;;
      3)
        echo ""
        echo -e "  ${CYAN}Interactive mode — y=ναι / n=όχι / q=τέλος${NC}"
        echo ""
        git add -p
        echo -e "  ${GREEN}✔ git add -p ολοκληρώθηκε${NC}"
        ;;
    esac

    git commit -m "$FULL_MSG"
    echo -e "  ${GREEN}✔ git commit \"$FULL_MSG\"${NC}"

    if [[ "$action_choice" == "1" ]]; then
      git push origin "$branch"
      echo -e "  ${GREEN}✔ git push origin $branch${NC}"
    fi
    ;;

  3) # Μόνο Push
    git push origin "$branch"
    echo -e "  ${GREEN}✔ git push origin $branch${NC}"
    ;;

esac

echo ""
divider
echo -e "  ${GREEN}${BOLD}🎉 Ολοκληρώθηκε!${NC}  ${YELLOW}[$SELECTED_NAME]${NC} → ${BLUE}$branch${NC}"
divider
echo ""
