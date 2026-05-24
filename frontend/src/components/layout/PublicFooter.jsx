import {
    Facebook,
    Github,
    Instagram,
    Linkedin,
    Mail,
    MapPin,
    PanelsTopLeft,
    MessageCircle,
    Phone,
    Send,
    Twitter
} from "lucide-react";
import {COMPANY_NAME} from "./BrandMark";
const socialLinks = [
    {href: "https://www.linkedin.com/in/manishrnl", label: "LinkedIn", Icon: Linkedin},
    {href: "https://github.com/manishrnl", label: "GitHub", Icon: Github},
    {href: "https://wa.me/919501421887", label: "WhatsApp", Icon: MessageCircle},
    {href: "https://www.instagram.com/manishrnl/", label: "Instagram", Icon: Instagram},
    {href: "https://www.facebook.com/profile.php?id=100011121437261", label: "Facebook", Icon: Facebook},
    {href: "https://x.com/manishrnl", label: "Twitter", Icon: Twitter}
];

const PublicFooter = () => <footer id="contact" className="border-t border-slate-200 bg-slate-950 text-white">
    <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid gap-8 md:grid-cols-[1.1fr_1fr_1.2fr]">
            <section>
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-teal-300">Let's connect</p>
                <h2 className="mt-3 text-2xl font-semibold">Build, review, or extend the platform.</h2>
                <p className="mt-4 max-w-sm text-sm leading-6 text-slate-300">
                    Open to collaboration on microservices, cloud deployment, auth systems, and polished full-stack products.
                </p>
            </section>

            <section>
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Contact me</p>
                <div className="mt-4 space-y-3 text-sm">
                    <a href="https://www.mappls.com/ctd6rr" target="_blank" rel="noreferrer" className="flex items-center gap-3 text-slate-300 transition hover:text-white">
                        <MapPin className="h-5 w-5 text-teal-300"/>
                        Madhubani, Bihar
                    </a>
                    <a href="mailto:manishrajrnl@zohomail.in" className="flex items-center gap-3 text-slate-300 transition hover:text-white">
                        <Mail className="h-5 w-5 text-rose-300"/>
                        manishrajrnl@zohomail.in
                    </a>
                    <a href="tel:+919501421887" className="flex items-center gap-3 text-slate-300 transition hover:text-white">
                        <Phone className="h-5 w-5 text-emerald-300"/>
                        +91 9501421887
                    </a>
                    <a href="https://www.manishrnl.in/projects" target="_blank" rel="noreferrer" className="flex items-center gap-3 text-slate-300 transition hover:text-white">
                        <PanelsTopLeft className="h-5 w-5 text-amber-300"/>
                        Portfolio projects
                    </a>
                </div>
            </section>

            <section>
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Reach me</p>
                <div className="mt-4 flex flex-wrap gap-2">
                    {socialLinks.map(({href, label, Icon}) => <a
                        key={label}
                        href={href}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex h-10 items-center gap-2 rounded-md border border-white/10 bg-white/[0.06] px-3 text-xs font-bold text-slate-100 transition hover:-translate-y-0.5 hover:border-teal-300/60 hover:bg-white/[0.1]"
                    >
                        <Icon className="h-4 w-4"/>
                        {label}
                    </a>)}
                </div>
            </section>
        </div>

        <div className="mt-10 flex flex-col gap-3 border-t border-white/10 pt-5 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between">
            <p>© 2026 {COMPANY_NAME}. All rights reserved.</p>
            <p className="inline-flex items-center gap-2">
                <Send className="h-3.5 w-3.5 text-teal-300"/>
                Designed by <strong className="text-amber-200">Manish</strong>
            </p>
        </div>
    </div>
</footer>;

export {
    PublicFooter
};
