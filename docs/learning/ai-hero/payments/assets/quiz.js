// Shared quiz widget for nl-portal teaching workspace lessons.
// Usage: <div class="quiz" data-correct="1">...options with class quiz-option...</div>
// Then call initQuizzes() once the DOM is ready.
function initQuizzes() {
  document.querySelectorAll(".quiz").forEach((quiz) => {
    const correctIndex = parseInt(quiz.dataset.correct, 10);
    const options = quiz.querySelectorAll(".quiz-option");
    const feedback = quiz.querySelector(".quiz-feedback");
    options.forEach((opt, i) => {
      opt.addEventListener("click", () => {
        if (quiz.dataset.answered) return;
        quiz.dataset.answered = "true";
        options.forEach((o, j) => {
          o.classList.add(j === correctIndex ? "correct" : i === j ? "incorrect" : "");
        });
        if (feedback) {
          feedback.textContent =
            i === correctIndex
              ? "Correct."
              : "Niet helemaal — de groene optie is juist.";
        }
      });
    });
  });
}
document.addEventListener("DOMContentLoaded", initQuizzes);
