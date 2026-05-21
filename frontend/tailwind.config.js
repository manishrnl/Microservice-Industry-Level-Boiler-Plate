export default {
    darkMode: "class",
    content: ["./index.html", "./src/**/*.{js,jsx}"],
    theme: {
        extend: {
            keyframes: {
                shake: {
                    "0%,100%": {transform: "rotate(0deg)"},
                    "20%": {transform: "rotate(-12deg)"},
                    "40%": {transform: "rotate(10deg)"},
                    "60%": {transform: "rotate(-8deg)"},
                    "80%": {transform: "rotate(6deg)"}
                }
            },
            animation: {
                shake: "shake 550ms ease-in-out"
            }
        }
    },
    plugins: []
};
