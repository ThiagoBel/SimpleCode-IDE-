#ifndef TASIL_H
#define TASIL_H

#include <string>

std::string print(const std::string &texto);
std::string printend(const std::string &textoend);
std::string println(const std::string &textoln);
std::string HelloWorld(const std::string &print);
std::string nfile(const std::string &name);
std::string nfilec(const std::string &namee, const std::string &content);
std::string cmd(const std::string &cmdd);
std::string ps(const std::string &cmddd);
std::string autorun(const std::string &execcmd);
std::string autorunnb(const std::string &execccmd);
std::string autorunnostd(const std::string &execccmd);
std::string versionCM(const std::string &chavezinha);
std::string helpCM(const std::string &chavezinhaa);
std::string ProcessTime();
std::string os();
std::string random(const int &min, const int &max);
std::string readfile(const std::string &filename);
std::string del(const std::string &nome);
std::string repeat(const std::string &texto, int vezes);
std::string wait(double seconds);
std::string datetime(const std::string &format);
std::string upper(const std::string &texto);
std::string lower(const std::string &texto);
std::string reverse(const std::string &texto);
std::string cls();
std::string getUsername();
std::string backpack_put(const std::string &idt, const std::string &text);
std::string backpack_open(const std::string &ido);
std::string backpack_remove(const std::string &idr);
bool backpack_find(const std::string &idf);
std::string random_characters(const int &quant);

bool tf(const bool &valor);
bool havefile(const std::string &filename);
bool contains(const std::string &texto, const std::string &sub);
bool is_windows();
bool is_linux();
bool is_android();
bool is_ios();
bool is_macos();
bool is_time(const std::string &format, const std::string &target_time);

template <typename T>
void debug(const std::string &label, T valor);

#endif